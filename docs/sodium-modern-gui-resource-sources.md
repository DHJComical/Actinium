# Modern Sodium GUI Resource Sources

This inventory records the resource boundary for the modern Sodium GUI integration. It is
an engineering provenance record, not legal advice. Distributors remain responsible for
evaluating the PolyForm Shield License 1.0.0 and all other applicable license obligations.

## Upstream baseline and license context

- Repository: <https://github.com/CaffeineMC/sodium>
- Commit: `3c4c4b29ad733ce3c20a1dddbb7f623d3897c82b`
- Upstream license at that commit: PolyForm Shield License 1.0.0
- Included license text: [`LICENSE-SODIUM.md`](../LICENSE-SODIUM.md)
- Public maintainer comment: <https://github.com/CaffeineMC/sodium/issues/2400#issuecomment-2038560656>

The linked issue comment states that ports targeting very old Minecraft versions do not compete
with Sodium. Actinium records that public statement as context for this Minecraft 1.12.2 port. It
does not amend the license, constitute legal advice, or replace a distributor's own assessment.

No additional plain-text `Required Notice:` or `Licensor Line of Business:` statement was present
in the Sodium repository at the recorded commit, apart from the examples contained in the license
text itself.

## Status: code and textures removed, language labels retained

The PolyForm Shield licensed `net.caffeinemc.mods.sodium.{api,client}.config` data model and the
`net.caffeinemc.mods.sodium.client.gui` settings screens were **removed entirely** from this
repository (see `docs/rso-port.md`). The settings UI is now provided by the MIT-licensed
Reese's Sodium Options port backed by the embeddium option model
(`org.embeddedt.embeddium.api.options.*` plus Actinium's self-authored extensions in
`celeritas-common/.../embeddium/api/options/`).

Only the language labels below remain incorporated from the modern Sodium resource tree:

| Upstream source | Actinium target | Adaptation | Purpose |
| --- | --- | --- | --- |
| `common/src/main/resources/assets/sodium/lang/en_us.json` (selected GUI keys) | `src/main/resources/assets/sodium/lang/en_us.lang` | Converted to the Minecraft 1.12.2 `.lang` format | General page, search, external-page, button and impact labels |
| Translation derived from the selected English GUI keys | `src/main/resources/assets/sodium/lang/zh_cn.lang` | Actinium translation | Simplified Chinese labels for the same GUI controls |

Previously recorded but now **removed** from this repository:

- `src/main/resources/config-icon.png` (classpath icon)
- `src/main/resources/assets/sodium/textures/gui/reset_button.png` (reset control icon)
- `src/main/resources/assets/sodium/textures/gui/tooltip_arrows.png` (scrollable tooltip arrows)

`coffee_cup.png` and the donation label are intentionally not incorporated because donation UI is
outside the port's defined scope. Renderer shaders from the modern Sodium resource tree are also
not incorporated here; Actinium keeps its existing renderer implementation and resources under
its own namespace.

## Legacy resource namespace migration

The following pre-existing renderer resources came from the legacy GPL-associated renderer tree.
They were moved out of `assets/sodium` so that the `sodium` namespace is reserved for the
language labels listed above.

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
namespace. None of these legacy resources remains available through `sodium:*`.