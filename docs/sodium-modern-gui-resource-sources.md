# Modern Sodium GUI Resource Sources

This inventory records the resource boundary for the modern Sodium GUI integration. It is
an engineering provenance record, not legal advice. Distributors remain responsible for
evaluating the PolyForm Shield License 1.0.0 and all other applicable license obligations.

## Upstream baseline and license context

- Repository: <https://github.com/CaffeineMC/sodium>
- Commit: `3c4c4b29ad733ce3c20a1dddbb7f623d3897c82b`
- Upstream license at that commit: PolyForm Shield License 1.0.0
- Public maintainer comment: <https://github.com/CaffeineMC/sodium/issues/2400#issuecomment-2038560656>

The linked issue comment states that ports targeting very old Minecraft versions do not compete
with Sodium. Actinium records that public statement as context for this Minecraft 1.12.2 port. It
does not amend the license, constitute legal advice, or replace a distributor's own assessment.

No additional plain-text `Required Notice:` or `Licensor Line of Business:` statement was present
in the Sodium repository at the recorded commit, apart from the examples contained in the license
text itself.

## Status: all PolyForm Shield resources removed

Every PolyForm Shield licensed artifact from the modern Sodium resource tree has been **removed
entirely** from this repository, including:

- `net.caffeinemc.mods.sodium.{api,client}.config` data model and
  `net.caffeinemc.mods.sodium.client.gui` settings screens (see `docs/rso-port.md`)
- `src/main/resources/config-icon.png` (classpath icon)
- `src/main/resources/assets/sodium/textures/gui/reset_button.png` (reset control icon)
- `src/main/resources/assets/sodium/textures/gui/tooltip_arrows.png` (scrollable tooltip arrows)
- `src/main/resources/assets/sodium/lang/en_us.lang` and `zh_cn.lang` (language labels)

The `LICENSE-SODIUM.md` notice text was removed together with the last PolyForm Shield resource.
The settings UI is now the MIT-licensed Reese's Sodium Options port backed by the embeddium
option model (`org.embeddedt.embeddium.api.options.*` plus Actinium's self-authored extensions in
`celeritas-common/.../embeddium/api/options/`). The GUI translation keys (`sodium.options.*`,
`celeritas.options.*`, `embeddium.options.*`) are carried by the celeritas namespace language
files, whose content is maintained in sync with the upstream Celeritas fork
(<https://git.taumc.org/embeddedt/celeritas>).

`coffee_cup.png` and the donation label are intentionally not incorporated because donation UI is
outside the port's defined scope. Renderer shaders from the modern Sodium resource tree are also
not incorporated here; Actinium keeps its existing renderer implementation and resources under
its own namespace.

## Legacy resource namespace migration

The following pre-existing renderer resources came from the legacy GPL-associated renderer tree.
They were moved out of `assets/sodium` so the `sodium` namespace never carries licensed GUI
content:

| Previous path | Current path |
| --- | --- |
| `src/main/resources/assets/sodium/shaders/blocks/block_layer_opaque.fsh` | `src/main/resources/assets/actinium/shaders/blocks/block_layer_opaque.fsh` |
| `src/main/resources/assets/sodium/shaders/blocks/block_layer_opaque.vsh` | `src/main/resources/assets/actinium/shaders/blocks/block_layer_opaque.vsh` |
| `src/main/resources/assets/sodium/shaders/clouds/clouds.fsh` | `src/main/resources/assets/actinium/shaders/clouds/clouds.fsh` |
| `src/main/resources/assets/sodium/shaders/clouds/clouds.vsh` | `src/main/resources/assets/actinium/shaders/clouds/clouds.vsh` |
| `src/main/resources/assets/sodium/shaders/include/chunk_material.glsl` | `src/main/resources/assets/actinium/shaders/include/chunk_material.glsl` |
| `src/main/resources/assets/sodium/shaders/include/chunk_matrices.glsl` | `src/main/resources/assets/actinium/shaders/include/chunk_matrices.glsl` |
| `src/main/resources/assets/sodium/shaders/include/chunk_vertex.glsl` | `src/main/resources/assets/actinium/shaders/include/chunk_vertex.glsl` |
| `src/main/resources/assets/sodium/shaders/include/fog.glsl` | `src/main/resources/assets/actinium/shaders/include/fog.glsl` |

The associated runtime shader identifiers and shader `#import` directives now use the `actinium`
namespace. No licensed content remains available through `sodium:*`.