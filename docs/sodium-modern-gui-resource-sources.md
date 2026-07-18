# Modern Sodium GUI Resource Sources

This inventory records the resource boundary established for the modern Sodium GUI port. It is
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

## Modern Sodium files incorporated

All upstream paths below are relative to the Sodium repository at the recorded commit.

| Upstream source | Actinium target | SHA-256 | Purpose |
| --- | --- | --- | --- |
| `common/src/main/resources/config-icon.png` | `src/main/resources/config-icon.png` | `CDEA65424B7676637B66F0BF2D65348F461F6055996D1AC60FC7ACB079F386C3` | Classpath icon registered by the Sodium configuration builder |
| `common/src/main/resources/assets/sodium/textures/gui/reset_button.png` | `src/main/resources/assets/sodium/textures/gui/reset_button.png` | `7484B46E2EE32286C5988E50467126B1AB1738AC1B61424DA3F893B72BFFDCED` | Reset control icon |
| `common/src/main/resources/assets/sodium/textures/gui/tooltip_arrows.png` | `src/main/resources/assets/sodium/textures/gui/tooltip_arrows.png` | `4319D07381D9175EE21B53CE9D5D410B99A9E7842A4D335E1B16F7AABADD49C3` | Scrollable tooltip arrows |
| `common/src/main/resources/assets/sodium/lang/en_us.json` (selected GUI keys) | `src/main/resources/assets/sodium/lang/en_us.lang` | Adapted to the Minecraft 1.12.2 `.lang` format | General page, search, and external-page labels |
| Translation derived from the selected English GUI keys | `src/main/resources/assets/sodium/lang/zh_cn.lang` | Actinium translation | Simplified Chinese labels for the same GUI controls |

`coffee_cup.png` and the donation label are intentionally not incorporated because donation UI is
outside the port's defined scope. Renderer shaders from the modern Sodium resource tree are also
not incorporated here; Actinium keeps its existing renderer implementation and resources under
its own namespace.

## Legacy resource namespace migration

The following pre-existing renderer resources came from the legacy GPL-associated renderer tree.
They were moved out of `assets/sodium` so that the `sodium` namespace is reserved for the modern
PolyForm Shield GUI resources listed above.

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
