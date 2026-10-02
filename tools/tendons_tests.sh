#!/bin/sh
# Reinforced Tendons multiplayer checks (Linux, needs Xvfb): a dedicated server and two clients,
# "Observer" (looks at the wearer) and "Wearer" (double jump and air dash in four directions).
# The same run in four client setups, since Punchy changes the first-person hands and Fresh
# Animations (with Entity Model Features and Entity Texture Features) changes the player model:
#
#   tools/tendons_tests.sh vanilla        no extra mods
#   tools/tendons_tests.sh punchy         Punchy
#   tools/tendons_tests.sh fresh          EMF, ETF and the Fresh Animations packs
#   tools/tendons_tests.sh punchy-fresh   all of them
#   tools/tendons_tests.sh all            the four, one after the other
#
# Screenshots and logs end up in build/tendons-mp/<setup>/{server,observer,wearer}. The mods are
# only installed on the clients: the server runs the same in every setup.
set -eu
cd "$(dirname "$0")/.."
ROOT=$(pwd)
PORT=${TENDONS_PORT:-25570}

sh ./gradlew writeDevRuntimeClasspath --console=plain --max-workers=1 -q
CP=$(cat build/dev-runtime-classpath.txt)
CFG="$ROOT/.gradle/loom-cache/launch.cfg"
MODS=build/mantis-compat/mods
PACKS=build/mantis-compat/resourcepacks

fetch() { [ -f "$2" ] || curl -sSfL -o "$2" "$1"; }

download() {
    mkdir -p "$MODS" "$PACKS"
    fetch https://cdn.modrinth.com/data/8aoMKplv/versions/80B8c9Qd/punchy-2.8c-fabric-1.21.1.jar "$MODS/punchy-2.8c-fabric-1.21.1.jar"
    fetch https://cdn.modrinth.com/data/4I1XuqiY/versions/FMPLuEEv/entity_model_features-3.3.9-1.21-fabric.jar "$MODS/entity_model_features-3.3.9-1.21-fabric.jar"
    fetch https://cdn.modrinth.com/data/BVzZfTc1/versions/lmZfRqnz/entity_texture_features-7.2.4-1.21-fabric.jar "$MODS/entity_texture_features-7.2.4-1.21-fabric.jar"
    fetch https://cdn.modrinth.com/data/50dA9Sha/versions/xN57JJts/FreshAnimations_v1.10.4.zip "$PACKS/FreshAnimations_v1.10.4.zip"
    fetch 'https://cdn.modrinth.com/data/YAVTU8mK/versions/RfJ3uz2J/FA%2BAll_Extensions-v1.8.1.zip' "$PACKS/FA+All_Extensions-v1.8.1.zip"
    fetch 'https://cdn.modrinth.com/data/TAIMVZCL/versions/Wj7NeGjP/FA%2BPlayer-v1.1.zip' "$PACKS/FA+Player-v1.1.zip"
}

# client_dir <dir> <punchy yes|no> <fresh yes|no>
client_dir() {
    rm -rf "$1" && mkdir -p "$1/mods" "$1/resourcepacks"
    packs='"vanilla","fabric"'
    if [ "$2" = yes ]; then cp "$MODS/punchy-2.8c-fabric-1.21.1.jar" "$1/mods/"; fi
    if [ "$3" = yes ]; then
        cp "$MODS/entity_model_features-3.3.9-1.21-fabric.jar" "$MODS/entity_texture_features-7.2.4-1.21-fabric.jar" "$1/mods/"
        cp "$PACKS"/*.zip "$1/resourcepacks/"
        packs='"vanilla","fabric","file/FreshAnimations_v1.10.4.zip","file/FA+All_Extensions-v1.8.1.zip","file/FA+Player-v1.1.zip"'
    fi
    printf 'onboardAccessibility:false\nfov:0.0\nrenderDistance:6\nresourcePacks:[%s]\n' "$packs" > "$1/options.txt"
}

run_client() { # <dir> <role> <username>
    xvfb-run -a -s "-screen 0 1280x720x24" java -Xmx2G -Dfabric.dli.config="$CFG" -Dfabric.dli.env=client \
        -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient -Dapogee.tendonsMpRole="$2" -cp "$CP" \
        net.fabricmc.devlaunchinjector.Main --username "$3" --gameDir "$ROOT/$1" \
        --quickPlayMultiplayer "localhost:$PORT" --width 1280 --height 720 > "$1/run.out" 2>&1
}

one() { # <setup> <punchy> <fresh>
    base=build/tendons-mp/$1
    rm -rf "$base" && mkdir -p "$base/server"
    printf 'eula=true\n' > "$base/server/eula.txt"
    printf 'online-mode=false\nenforce-secure-profile=false\nspawn-protection=0\nlevel-type=minecraft\\:flat\ngenerate-structures=false\nmotd=tendons\nserver-port=%s\n' "$PORT" > "$base/server/server.properties"
    client_dir "$base/observer" "$2" "$3"
    client_dir "$base/wearer" "$2" "$3"
    (cd "$base/server" && exec java -Xmx2G -Dfabric.dli.config="$CFG" -Dfabric.dli.env=server \
        -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotServer -Dapogee.tendonsMp=true \
        -cp "$CP" net.fabricmc.devlaunchinjector.Main nogui > run.out 2>&1) &
    server=$!
    until grep -q 'Done (' "$base/server/run.out" 2>/dev/null; do
        kill -0 "$server" 2>/dev/null || { echo "server exited"; tail -40 "$base/server/run.out"; exit 1; }
        sleep 2
    done
    run_client "$base/observer" observer Observer &
    observer=$!
    sleep 20
    run_client "$base/wearer" wearer Wearer &
    wearer=$!
    wait "$observer" "$wearer" || true
    # The server stops itself when the script ends, but not if a client never joined.
    (sleep 60; kill "$server" 2>/dev/null) &
    wait "$server" || true
    echo "== $1"
    grep -h -E 'Loading [0-9]+ mods' "$base/observer/run.out" "$base/wearer/run.out" | sed 's/.*INFO\] //' | sort -u || true
    grep -h 'TendonsMultiplayer' "$base"/server/run.out "$base"/observer/run.out "$base"/wearer/run.out | grep -E 'FAILED|passed|failed' || true
}

download
case "${1:-}" in
vanilla) one vanilla no no ;;
punchy) one punchy yes no ;;
fresh) one fresh no yes ;;
punchy-fresh) one punchy-fresh yes yes ;;
all) one vanilla no no; one punchy yes no; one fresh no yes; one punchy-fresh yes yes ;;
*) sed -n '2,12p' "$0"; exit 2 ;;
esac
