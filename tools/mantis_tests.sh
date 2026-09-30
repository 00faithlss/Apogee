#!/bin/sh
# Mantis Blades checks that need several Minecraft processes or extra mods (Linux, needs Xvfb).
#
#   tools/mantis_tests.sh mp      dedicated server + two clients ("Observer", "Wearer")
#   tools/mantis_tests.sh compat  runMantisSmoke with Punchy, EMF, ETF and Fresh Animations
#   tools/mantis_tests.sh motion  first-person arm motion, every tick, in vanilla and with Punchy
#   tools/mantis_tests.sh calibrate  fits the folded implant onto the hand that follows it
#   tools/mantis_tests.sh iris    the Mantis smoke script with Sodium, Iris and the MakeUp UltraFast
#                                 shader pack, in a production-like instance (Iris fails in dev runs)
#
# Screenshots and logs end up in build/mantis-mp, build/mantis-compat, build/mantis-motion and
# build/mantis-iris.
set -eu
cd "$(dirname "$0")/.."
ROOT=$(pwd)

# The iris mode resolves Sodium and Iris too (see build.gradle).
case "${1:-}" in iris) EXTRA=-PirisTest ;; *) EXTRA= ;; esac
sh ./gradlew writeDevRuntimeClasspath --console=plain --max-workers=1 -q $EXTRA
CP=$(cat build/dev-runtime-classpath.txt)
CFG="$ROOT/.gradle/loom-cache/launch.cfg"

# server <dir>: dedicated server running the multiplayer script, in the background.
server() {
    (cd "$1" && exec java -Xmx2G -Dfabric.dli.config="$CFG" -Dfabric.dli.env=server \
        -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotServer -Dapogee.mpSmoke=true \
        -cp "$CP" net.fabricmc.devlaunchinjector.Main nogui > run.out 2>&1) &
}

client_options() {
    mkdir -p "$1"
    printf 'onboardAccessibility:false\nfov:0.0\nrenderDistance:6\n%s\n' "${2:-}" > "$1/options.txt"
}

case "${1:-}" in
mp)
    base=build/mantis-mp
    rm -rf "$base" && mkdir -p "$base/server"
    printf 'eula=true\n' > "$base/server/eula.txt"
    printf 'online-mode=false\nenforce-secure-profile=false\nspawn-protection=0\nlevel-type=minecraft\\:flat\ngenerate-structures=false\nmotd=mantis\nserver-port=25565\n' > "$base/server/server.properties"
    client_options "$base/observer"
    client_options "$base/wearer"
    server "$base/server"
    server=$!
    # Wait for the server before the clients try to join.
    until grep -q 'Done (' "$base/server/run.out" 2>/dev/null; do
        kill -0 "$server" 2>/dev/null || { echo "server exited"; tail -40 "$base/server/run.out"; exit 1; }
        sleep 2
    done
    xvfb-run -a -s "-screen 0 1280x720x24" java -Xmx2G -Dfabric.dli.config="$CFG" -Dfabric.dli.env=client \
        -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient -Dapogee.mpRole=observer -cp "$CP" \
        net.fabricmc.devlaunchinjector.Main --username Observer --gameDir "$ROOT/$base/observer" \
        --quickPlayMultiplayer localhost:25565 --width 1280 --height 720 > "$base/observer/run.out" 2>&1 &
    observer=$!
    sleep 15
    xvfb-run -a -s "-screen 0 1280x720x24" java -Xmx2G -Dfabric.dli.config="$CFG" -Dfabric.dli.env=client \
        -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient -Dapogee.mpRole=wearer -cp "$CP" \
        net.fabricmc.devlaunchinjector.Main --username Wearer --gameDir "$ROOT/$base/wearer" \
        --quickPlayMultiplayer localhost:25565 --width 1280 --height 720 > "$base/wearer/run.out" 2>&1 &
    wearer=$!
    wait "$observer" "$wearer" || true
    # The server stops itself when the script ends, but not if a client never joined.
    (sleep 60; kill "$server" 2>/dev/null) &
    wait "$server" || true
    grep -h 'MultiplayerSmoke' "$base"/*/run.out | grep -E 'FAILED|passed|failed' || true
    ;;
compat)
    base=build/mantis-compat
    mkdir -p "$base/mods" "$base/resourcepacks"
    fetch() { [ -f "$2" ] || curl -sSfL -o "$2" "$1"; }
    fetch https://cdn.modrinth.com/data/8aoMKplv/versions/80B8c9Qd/punchy-2.8c-fabric-1.21.1.jar "$base/mods/punchy-2.8c-fabric-1.21.1.jar"
    fetch https://cdn.modrinth.com/data/4I1XuqiY/versions/FMPLuEEv/entity_model_features-3.3.9-1.21-fabric.jar "$base/mods/entity_model_features-3.3.9-1.21-fabric.jar"
    fetch https://cdn.modrinth.com/data/BVzZfTc1/versions/lmZfRqnz/entity_texture_features-7.2.4-1.21-fabric.jar "$base/mods/entity_texture_features-7.2.4-1.21-fabric.jar"
    fetch https://cdn.modrinth.com/data/50dA9Sha/versions/xN57JJts/FreshAnimations_v1.10.4.zip "$base/resourcepacks/FreshAnimations_v1.10.4.zip"
    fetch 'https://cdn.modrinth.com/data/YAVTU8mK/versions/RfJ3uz2J/FA%2BAll_Extensions-v1.8.1.zip' "$base/resourcepacks/FA+All_Extensions-v1.8.1.zip"
    fetch 'https://cdn.modrinth.com/data/TAIMVZCL/versions/Wj7NeGjP/FA%2BPlayer-v1.1.zip' "$base/resourcepacks/FA+Player-v1.1.zip"
    rm -rf "$base/screenshots"
    client_options "$base" 'resourcePacks:["vanilla","fabric","file/FreshAnimations_v1.10.4.zip","file/FA+All_Extensions-v1.8.1.zip","file/FA+Player-v1.1.zip"]'
    xvfb-run -a -s "-screen 0 1600x900x24" java -Xmx3G -Dfabric.dli.config="$CFG" -Dfabric.dli.env=client \
        -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient -Dapogee.mantisSmoke=true -cp "$CP" \
        net.fabricmc.devlaunchinjector.Main --gameDir "$ROOT/$base" --width 1600 --height 900 > "$base/run.out" 2>&1 || true
    grep -h -E 'Loading [0-9]+ mods|MantisSmoke.*(FAILED|passed|failed)' "$base/run.out" || true
    ;;
motion|calibrate)
    # The same run twice: vanilla hands, then with Punchy alone. Motion frames are then tracked
    # with tools/mantis_motion.py; the calibration logs the fitted folded pose.
    MODE=$1
    if [ "$MODE" = motion ]; then PROP=Motion; else PROP=Calibrate; fi
    # Fixed names give fixed default skins: "Dev" wears sleeves.
    for kind in vanilla punchy; do
        base=build/mantis-$MODE/$kind
        rm -rf "$base" && mkdir -p "$base/mods"
        if [ "$kind" = punchy ]; then
            mkdir -p build/mantis-compat/mods
            fetch() { [ -f "$2" ] || curl -sSfL -o "$2" "$1"; }
            fetch https://cdn.modrinth.com/data/8aoMKplv/versions/80B8c9Qd/punchy-2.8c-fabric-1.21.1.jar build/mantis-compat/mods/punchy-2.8c-fabric-1.21.1.jar
            cp build/mantis-compat/mods/punchy-2.8c-fabric-1.21.1.jar "$base/mods/"
        fi
        client_options "$base"
        xvfb-run -a -s "-screen 0 960x540x24" java -Xmx3G -Dfabric.dli.config="$CFG" -Dfabric.dli.env=client \
            -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient -Dapogee.mantis$PROP=true -cp "$CP" \
            net.fabricmc.devlaunchinjector.Main --username "${MOTION_NAME:-Dev}" --gameDir "$ROOT/$base" --width 960 --height 540 > "$base/run.out" 2>&1 || true
        grep -h -E 'Loading [0-9]+ mods|motion study done|calibrate (start|best)' "$base/run.out" || true
    done
    ;;
iris)
    # A production-like instance (intermediary names, as in a launcher), where Iris works:
    # the built mod, its dependencies, Sodium, Iris, a shader pack and the smoke script jar.
    base=build/mantis-iris
    rm -rf "$base/mods" "$base/screenshots" "$base/crash-reports"
    mkdir -p "$base/mods" "$base/shaderpacks" "$base/config"
    sh ./gradlew build remapSmokeJar -x test --console=plain -q $EXTRA
    jar() { find "$HOME/.gradle/caches/modules-2/files-2.1/$1" -name "$2" ! -name '*sources*' | head -1; }
    for j in "$(jar net.fabricmc.fabric-api/fabric-api 'fabric-api-*.jar')" "$(jar maven.modrinth/trinkets 'trinkets-*.jar')" \
        "$(jar software.bernie.geckolib 'geckolib-*.jar')" "$(jar org.ladysnake.cardinal-components-api/cardinal-components-base '*.jar')" \
        "$(jar org.ladysnake.cardinal-components-api/cardinal-components-entity '*.jar')" "$(jar maven.modrinth/simply-tooltips '*.jar')" \
        "$(jar maven.modrinth/architectury-api '*.jar')" "$(jar maven.modrinth/fzzy-config '*.jar')" \
        "$(jar net.fabricmc/fabric-language-kotlin '*.jar')" "$(jar maven.modrinth/sodium '*.jar')" "$(jar maven.modrinth/iris '*.jar')"; do
        cp "$j" "$base/mods/"
    done
    cp "build/libs/apogee-$(sed -n 's/^mod_version=//p' gradle.properties).jar" "build/smoke/"*-smoke.jar "$base/mods/"
    fetch() { [ -f "$2" ] || curl -sSfL -o "$2" "$1"; }
    fetch https://cdn.modrinth.com/data/izsIPI7a/versions/T3EhqZo1/MakeUp-UltraFast-9.5e.zip "$base/shaderpacks/MakeUp-UltraFast-9.5e.zip"
    printf 'enableShaders=true\nshaderPack=MakeUp-UltraFast-9.5e.zip\n' > "$base/config/iris.properties"
    client_options "$base" 'resourcePacks:["vanilla","fabric"]'
    # Libraries from the dev classpath, minus the named game, dev tooling and dev-remapped mods.
    PCP=$(echo "$CP" | tr ':' '\n' | grep '\.jar$' | grep -v -E 'minecraft-merged|/remapped_mods/|dev-launch-injector|mappings\.jar|kotlin|atomicfu|tomlkt|jankson|annotations-' | tr '\n' ':')
    PCP="$PCP$(jar net.fabricmc/intermediary '*.jar'):$HOME/.gradle/caches/fabric-loom/1.21.1/minecraft-client.jar"
    xvfb-run -a -s "-screen 0 1600x900x24" java -Xmx3G -Dapogee.mantisSmoke=true -cp "$PCP" net.fabricmc.loader.impl.launch.knot.KnotClient \
        --gameDir "$ROOT/$base" --assetsDir "$HOME/.gradle/caches/fabric-loom/assets" --assetIndex 1.21.1-17 --version 1.21.1 \
        --accessToken 0 --username Dev --width 1600 --height 900 > "$base/run.out" 2>&1 || true
    grep -h -E 'Loading [0-9]+ mods|Using shaderpack|Mantis smoke run|MantisSmoke.*(FAILED|passed|failed)' "$base/run.out" || true
    ;;
*)
    sed -n '2,9p' "$0"
    exit 2
    ;;
esac
