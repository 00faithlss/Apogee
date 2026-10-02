#!/bin/sh
# Blood Pump multiplayer check (Linux, needs Xvfb): a dedicated server and two clients ("Observer",
# "Wearer"), in four client setups.
#
#   tools/bloodpump_tests.sh [vanilla|punchy|fresh|both]...   (all four when none is given)
#
# vanilla  no extra mods; punchy  Punchy 2.8c; fresh  Entity Model Features, Entity Texture Features
# and the Fresh Animations packs; both  Punchy and Fresh Animations together.
# Screenshots and logs end up in build/bloodpump-mp/<setup>/{observer,wearer,server}.
set -eu
cd "$(dirname "$0")/.."
ROOT=$(pwd)
sh ./gradlew writeDevRuntimeClasspath --console=plain --max-workers=1 -q
CP=$(cat build/dev-runtime-classpath.txt)
CFG="$ROOT/.gradle/loom-cache/launch.cfg"
CACHE=build/mantis-compat
fetch() { [ -f "$2" ] || curl -sSfL -o "$2" "$1"; }
mkdir -p "$CACHE/mods" "$CACHE/resourcepacks"
fetch https://cdn.modrinth.com/data/8aoMKplv/versions/80B8c9Qd/punchy-2.8c-fabric-1.21.1.jar "$CACHE/mods/punchy-2.8c-fabric-1.21.1.jar"
fetch https://cdn.modrinth.com/data/4I1XuqiY/versions/FMPLuEEv/entity_model_features-3.3.9-1.21-fabric.jar "$CACHE/mods/entity_model_features-3.3.9-1.21-fabric.jar"
fetch https://cdn.modrinth.com/data/BVzZfTc1/versions/lmZfRqnz/entity_texture_features-7.2.4-1.21-fabric.jar "$CACHE/mods/entity_texture_features-7.2.4-1.21-fabric.jar"
fetch https://cdn.modrinth.com/data/50dA9Sha/versions/xN57JJts/FreshAnimations_v1.10.4.zip "$CACHE/resourcepacks/FreshAnimations_v1.10.4.zip"
fetch 'https://cdn.modrinth.com/data/YAVTU8mK/versions/RfJ3uz2J/FA%2BAll_Extensions-v1.8.1.zip' "$CACHE/resourcepacks/FA+All_Extensions-v1.8.1.zip"
fetch 'https://cdn.modrinth.com/data/TAIMVZCL/versions/Wj7NeGjP/FA%2BPlayer-v1.1.zip' "$CACHE/resourcepacks/FA+Player-v1.1.zip"

# client <dir> <role> <setup>: one client of the run, in the background.
client() {
    dir=$1; role=$2; setup=$3
    mkdir -p "$dir/mods" "$dir/resourcepacks"
    packs='"vanilla","fabric"'
    case "$setup" in punchy|both) cp "$CACHE/mods/punchy-2.8c-fabric-1.21.1.jar" "$dir/mods/" ;; esac
    case "$setup" in fresh|both)
        cp "$CACHE"/mods/entity_*.jar "$dir/mods/"
        cp "$CACHE"/resourcepacks/*.zip "$dir/resourcepacks/"
        packs='"vanilla","fabric","file/FreshAnimations_v1.10.4.zip","file/FA+All_Extensions-v1.8.1.zip","file/FA+Player-v1.1.zip"' ;;
    esac
    printf 'onboardAccessibility:false\nfov:0.0\nrenderDistance:6\nresourcePacks:[%s]\n' "$packs" > "$dir/options.txt"
    xvfb-run -a -s "-screen 0 1280x720x24" java -Xmx2G -Dfabric.dli.config="$CFG" -Dfabric.dli.env=client \
        -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient -Dapogee.bpMpRole="$role" -cp "$CP" \
        net.fabricmc.devlaunchinjector.Main --username "$(echo "$role" | sed 's/./\U&/')" --gameDir "$ROOT/$dir" \
        --quickPlayMultiplayer localhost:25565 --width 1280 --height 720 > "$dir/run.out" 2>&1 &
}

for setup in ${*:-vanilla punchy fresh both}; do
    base=build/bloodpump-mp/$setup
    rm -rf "$base" && mkdir -p "$base/server"
    printf 'eula=true\n' > "$base/server/eula.txt"
    printf 'online-mode=false\nenforce-secure-profile=false\nspawn-protection=0\nlevel-type=minecraft\\:flat\ngenerate-structures=false\nmotd=bloodpump\nserver-port=25565\n' > "$base/server/server.properties"
    (cd "$base/server" && exec java -Xmx2G -Dfabric.dli.config="$CFG" -Dfabric.dli.env=server \
        -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotServer -Dapogee.bpMpSmoke=true \
        -cp "$CP" net.fabricmc.devlaunchinjector.Main nogui > run.out 2>&1) &
    server=$!
    until grep -q 'Done (' "$base/server/run.out" 2>/dev/null; do
        kill -0 "$server" 2>/dev/null || { echo "server exited"; tail -40 "$base/server/run.out"; exit 1; }
        sleep 2
    done
    client "$base/observer" observer "$setup"
    observer=$!
    sleep 15
    client "$base/wearer" wearer "$setup"
    wearer=$!
    wait "$observer" "$wearer" || true
    (sleep 60; kill "$server" 2>/dev/null) &
    wait "$server" || true
    echo "== $setup"
    grep -h 'BloodPumpMultiplayer' "$base/server/run.out" "$base"/*/run.out | grep -E 'FAILED|passed|failed' || echo "no result"
done
