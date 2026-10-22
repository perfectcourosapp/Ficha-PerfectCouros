# Ficha PerfectCouros

Este repositório reúne os dois projetos:

- **Android V36**: projeto Android na raiz (`app/`, `build.gradle`, `settings.gradle`, `.github/workflows/gerar-apk.yml`). Não foi alterado.
- **iPhone PWA V7**: arquivos em `iOS-PWA/`, derivados do HTML iOS V6 Seta Única.

## Publicar a PWA no GitHub Pages

1. Envie o conteúdo **deste ZIP** para a raiz do repositório `perfectcourosapp/Ficha-PerfectCouros`, preservando as pastas (inclusive `.github`).
2. No GitHub, abra **Settings > Pages > Build and deployment > Source** e escolha **GitHub Actions**.
3. Em **Actions**, execute o fluxo **Publicar PWA iPhone** se ele não iniciar automaticamente.
4. Depois da publicação, abra no Safari do iPhone: `https://perfectcourosapp.github.io/Ficha-PerfectCouros/`.
5. No Safari, use **Compartilhar > Adicionar à Tela de Início**. Abra o app pelo ícone criado.

O cache offline funciona após a primeira visita com internet e instalação do service worker. Alguns recursos que dependam de rede ou de serviços do navegador podem continuar precisando de conexão. O GitHub Pages deve ser público ou estar disponível conforme as regras do plano GitHub.

**Importante:** este pacote preserva o código do Android V36 fornecido. A versão iOS V7 acrescenta metadados PWA, ícones e cache offline ao HTML V6. Teste no iPhone antes de considerar a versão aprovada.
