# neto

Registro de implementações e experimentos: https://zqovark.github.io/

Este repositório recebe os arquivos estáticos gerados pelo blog. A publicação é automática.

## Tema Astral

Adaptação do [Astral para Omarchy](https://github.com/brmnl/omarchy-astral-theme),
de Manuel Burki: superfícies quase pretas, bordas índigo, detalhes violeta e verde neon.

- Estilos compartilhados: `assets/astral-theme.css`, carregados depois do CSS gerado em `_astro/`.
- Wallpaper original: `images/astral/astral.webp`, distribuído com a [licença MIT do autor](assets/licenses/astral-MIT.txt).
- Fonte local: [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono), sob [SIL Open Font License](fonts/jetbrains-mono/OFL.txt).
- Cores dos exemplos de código adaptadas à paleta Astral nos HTMLs dos artigos.

O tema é estático, funciona sem JavaScript e respeita a preferência por movimento reduzido.
Busca, tradução e previews continuam usando os scripts existentes do blog.

Ao gerar novamente o site a partir de outro repositório, preserve a referência ao CSS
Astral, os assets e as cores de código; este repositório contém apenas o resultado estático.
