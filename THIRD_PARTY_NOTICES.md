# Third-Party Notices

Actinium contains source code adapted from other projects. The root `LICENSE` applies to
Actinium's original work; it does not replace notices or license terms attached to imported
files. When a source file carries its own copyright or license header, that header takes
precedence for that file.

This inventory describes the code currently embedded in the repository. It is not a substitute
for the full license text or legal advice.

## Embedded source trees

| Component | Repository | Local paths | Upstream license | Actinium import record |
| --- | --- | --- | --- | --- |
| Angelica / GLSM | https://github.com/GTNewHorizons/Angelica | `glsm/`, portions of `src/main/java/com/gtnewhorizons/angelica/`, Angelica resources | LGPL-3.0, with additional file-level notices | GLSM introduced in `119f607`; expanded with GTNHLib in `83ea1b7` |
| GTNHLib | https://github.com/GTNewHorizons/GTNHLib | `GTNHLib/` | LGPL-3.0, plus file-level notices | `83ea1b7` |
| Iris | https://github.com/IrisShaders/Iris | `shader/src/main/java/net/coderbot/iris/`, Iris API/resources, Iris-oriented mixins | LGPL-3.0 | full pipeline import in `79306ba` |
| Celeritas renderer | historical Celeritas/Embeddium/Sodium-derived code | `celeritas-common/`, portions of `src/main/java/org/taumc/` and `src/main/java/org/embeddedt/` | mixed upstream provenance; preserve file-level notices and consult source history | present in standalone extraction `6167e49`, split to its current tree in `2835afe` |
| LWJGL utility code | https://github.com/LWJGL/lwjgl3 | selected `GTNHLib/.../bytebuf/` and GL debug helpers | BSD-style LWJGL license, identified in file headers | imported through GTNHLib/GLSM; service layer work in `4826cf8` |
| GLSL Transformation Library | https://github.com/GTNewHorizons/glsl-transformation-lib | binary contained dependency | consult the dependency's published license and POM | Maven coordinate `org.taumc:glsl-transformation-lib` |

The exact upstream commit SHA used for the initial Angelica, GTNHLib, Iris, and Celeritas
imports was not recorded in this repository. The Actinium commits above are the reproducible
local import boundaries. Recovering and recording exact upstream SHAs is required before the
next bulk upstream refresh.

## Additional incorporated work

- Parts of the renderer retain package names and implementation ideas from Embeddium and Sodium.
- Several GTNHLib byte-buffer files carry explicit LWJGL copyright and license notices.
- `MergeSort.java` carries Apache-2.0 and CERN notices in its source header.
- ASM-derived utility code retains its upstream copyright notice.
- Shader compatibility behavior is informed by OptiFine/ShadersMod conventions; shader packs
  themselves are not distributed by this repository.

## Binary dependencies

The build also resolves libraries and optional Minecraft mods from Maven, Modrinth, and
CurseMaven. Their artifacts are governed by their own licenses. Dependencies placed in the
`contain` configuration are packaged as nested jars and must keep their original metadata.
See `gradle/scripts/dependencies.gradle` and `build.gradle` for the authoritative coordinates.

## Maintenance rule

For any future imported code:

1. Record the upstream repository, exact commit/tag, local paths, license, and Actinium import
   commit in this file.
2. Preserve upstream headers and include any required license or notice text in distributions.
3. Keep upstream synchronization separate from Actinium-specific behavior changes when practical.
4. Do not infer a license from package names or from Actinium's root license.

See `docs/upstream-maintenance.md` for the synchronization procedure and known provenance gaps.
