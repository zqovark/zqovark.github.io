# neto

Registro de implementações e experimentos: https://zqovark.github.io/

Este repositório contém o site estático. O GitHub Pages publica a raiz da branch `main`.

## Bellerophon

A identidade usa a paleta do [Bellerophon para Omarchy](https://github.com/janooh37-hue/omarchy-bellerophon-theme), de Ahmed Al Ali: azul profundo, mármore, bronze e vermelho discreto. O wallpaper `09-sea-sentinel.webp` é distribuído com a [licença MIT original](assets/licenses/bellerophon-MIT.txt).

- `assets/bellerophon.css`: layout, tipografia, paleta, responsividade e efeitos decorativos.
- `assets/bellerophon.js`: controle de pausa dos efeitos, preferência salva e respeito a `prefers-reduced-motion` e à visibilidade da página.
- `images/bellerophon/sea-sentinel.webp`: ilustração original, sem alterações no arquivo.
- `fonts/bellerophon/`: Cinzel (títulos), Source Serif 4 (leitura) e IBM Plex Mono (navegação e código), hospedadas localmente com suas licenças SIL OFL.

A navegação é horizontal. Na página inicial, escritos e projetos formam duas colunas; no celular, aparecem em sequência. Artigos usam uma coluna central de leitura.

Os tratamentos de glitch foram escritos em CSS: recortes deslocados, dithering e linhas de CRT, com referência à pesquisa visual de [endlessmazin no Tumblr](https://endlessmazin.tumblr.com/). Nenhuma obra do artista é redistribuída. Os efeitos ficam na ilustração e na marca, sem deslocar o conteúdo dos artigos. Podem ser pausados e são desativados com movimento reduzido.

O layout e a navegação funcionam sem JavaScript. Busca, tradução e previews mantêm os scripts existentes. O CSS legado em `_astro/` não é carregado pelo tema atual.

Ao regenerar o site a partir de outro repositório, preserve o layout atualizado dos HTMLs, os assets Bellerophon, as referências no `<head>` e as cores dos exemplos de código. Este repositório contém apenas o resultado estático.
