# Reese's Sodium Options License (MIT)

The Reese's Sodium Options UI (`me.flashyreese.mods.reeses_sodium_options.*`,
`assets/reeses-sodium-options/*`) embedded in this project is ported from
[FlashyReese/reeses-sodium-options](https://github.com/FlashyReese/reeses-sodium-options)
and remains licensed under the MIT License:

> The MIT License (MIT)
>
> Copyright (c) 2021 FlashyReese
>
> Permission is hereby granted, free of charge, to any person obtaining a copy
> of this software and associated documentation files (the "Software"), to deal
> in the Software without restriction, including without limitation the rights
> to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
> copies of the Software, and to permit persons to whom the Software is
> furnished to do so, subject to the following conditions:
>
> The above copyright notice and this permission notice shall be included in
> all copies or substantial portions of the Software.
>
> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
> IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
> FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
> AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
> LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
> OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
> THE SOFTWARE.

Porting notes:

- Donation prompt, Ko-fi support button and their config entries were removed
  (see `docs/rso-port.md`).
- The 1.12.2 port replaces the newer Minecraft GUI event model with the
  compatibility layer in `com.dhj.actinium.gui.rso.compat`; visual constants,
  layout math and widget behavior are kept verbatim.
- The option data layer no longer touches the (removed) `net.caffeinemc`
  configuration model: RSO rows read from the embeddium option model
  (`org.embeddedt.embeddium.api.options.*`) through the self-authored
  `RsoOption` / `RsoModOptions` wrappers. The embeddium API extensions added
  for this port (`ExternalPage`, `ExternalButtonControl`, option pending/applied
  defaults, group headers) are Actinium's own MIT-licensed additions.
