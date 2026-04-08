import assert from "node:assert/strict";
import { buildUrls, parseSearchResults, selectFileFromFilesPage, verifyFilePage } from "./curseforge-helper-core.js";

const searchFixture = `
[](/minecraft/mc-mods/jade "Go to Jade Project Page")

[Jade](/minecraft/mc-mods/jade "Go to Jade Project Page")

By[Snownee](/members/snownee)

Shows information about what you are looking at.

[](/minecraft/mc-mods/jade-addons "Go to Jade Addons (Neo/Forge) Project Page")

[Jade Addons (Neo/Forge)](/minecraft/mc-mods/jade-addons "Go to Jade Addons (Neo/Forge) Project Page")

By[Snownee](/members/snownee)

Jade additional supports for Neo/Forge
`;

const filesFixture = `
Type

Name

Uploaded

[ 
R

[NeoForge 1.21.1] 15.10.5

Jan 29, 2026

708.7 KB

-   1.21.1

-   NeoForge

](/minecraft/mc-mods/jade/files/7545219)

[ 
B

[NeoForge 1.21.1] 15.10.4

Jan 7, 2026

708.7 KB

-   1.21.1

-   NeoForge

](/minecraft/mc-mods/jade/files/7428884)
`;

const fileFixture = `
# Jade

By[Snownee](/members/snownee)

Project ID

324717

## [NeoForge 1.21.1] 15.10.5

### File Name

Jade-1.21.1-NeoForge-15.10.5.jar

### Supported Versions

- 1.21.1

### NeoForge

implementation "curse.maven:jade-324717:7545219"
`;

const urls = buildUrls({ query: "jade", minecraftVersion: "1.21.1", loader: "neoforge", hintSlug: "jade" });
assert.equal(urls.filesUrl, "https://www.curseforge.com/minecraft/mc-mods/jade/files/all?page=1&pageSize=20&version=1.21.1&gameVersionTypeId=6&showAlphaFiles=show");

const search = parseSearchResults({ query: "jade", text: searchFixture });
assert.equal(search.status, "resolved");
assert.equal(search.selectedProject.slug, "jade");

const files = selectFileFromFilesPage({ query: "jade", hintSlug: "jade", minecraftVersion: "1.21.1", loader: "neoforge", text: filesFixture });
assert.equal(files.status, "resolved");
assert.equal(files.selectedFile.fileId, 7545219);
assert.equal(files.selectedFile.releaseType, "R");

const verify = verifyFilePage({ query: "jade", hintSlug: "jade", hintProjectId: 324717, minecraftVersion: "1.21.1", loader: "neoforge", expectedFileId: 7545219, text: fileFixture });
assert.equal(verify.status, "resolved");
assert.equal(verify.file.fileName, "Jade-1.21.1-NeoForge-15.10.5.jar");

console.log("curseforge-helper-core tests passed");
