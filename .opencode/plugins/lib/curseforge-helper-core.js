import fs from "node:fs/promises";
import path from "node:path";

const CURSEFORGE_BASE_URL = "https://www.curseforge.com";

const LOADER_ALIASES = {
  forge: { name: "Forge", id: 1 },
  fabric: { name: "Fabric", id: 4 },
  quilt: { name: "Quilt", id: 5 },
  neoforge: { name: "NeoForge", id: 6 },
};

const RELEASE_PRIORITY = { R: 3, B: 2, A: 1 };

function normalizeText(value = "") {
  return value
    .normalize("NFKD")
    .replace(/\p{Diacritic}/gu, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .trim()
    .replace(/\s+/g, " ");
}

function slugify(value = "") {
  return normalizeText(value).replace(/ /g, "-");
}

function unique(values) {
  return [...new Set(values.filter(Boolean))];
}

function normalizeLoader(value = "") {
  const normalized = normalizeText(value).replace(/ /g, "");
  if (["neo", "neo-forge", "neoforge"].includes(normalized)) {
    return "neoforge";
  }
  if (["forge"].includes(normalized)) {
    return "forge";
  }
  if (["fabric"].includes(normalized)) {
    return "fabric";
  }
  if (["quilt"].includes(normalized)) {
    return "quilt";
  }
  return null;
}

function loaderDisplayName(loader) {
  return loader ? LOADER_ALIASES[loader]?.name ?? loader : null;
}

function escapeRegex(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function stripMarkdownArtifacts(line) {
  return line
    .replace(/^[-*]\s+/, "")
    .replace(/^#+\s*/, "")
    .replace(/^\[\s*$/, "")
    .replace(/^\]\([^)]*\)$/, "")
    .replace(/^!\[[^\]]*\]\([^)]*\)$/, "")
    .trim();
}

function meaningfulLines(block) {
  return block
    .split(/\r?\n/)
    .map(stripMarkdownArtifacts)
    .filter((line) => line && !/^Download( file)?$/i.test(line) && !/^Install( with CurseForge app)?$/i.test(line));
}

function parseCurseForgeInput(input = "") {
  const trimmed = input.trim();
  if (!trimmed) {
    return {};
  }

  const numeric = /^\d+$/.test(trimmed);
  if (numeric) {
    return { projectId: Number(trimmed), inputType: "project-id" };
  }

  try {
    const url = new URL(trimmed);
    if (url.hostname.includes("curseforge.com")) {
      const match = url.pathname.match(/\/minecraft\/mc-mods\/([^/]+)/i);
      const fileMatch = url.pathname.match(/\/minecraft\/mc-mods\/([^/]+)\/files\/(\d+)/i);
      return {
        inputType: "url",
        url: trimmed,
        slug: fileMatch?.[1] ?? match?.[1] ?? null,
        fileId: fileMatch ? Number(fileMatch[2]) : null,
      };
    }
  } catch {
    // ignore non-url input
  }

  if (/^[a-z0-9-]+$/i.test(trimmed) && trimmed.includes("-")) {
    return { slug: trimmed.toLowerCase(), inputType: "slug" };
  }

  return { inputType: "name" };
}

export function normalizeQuery(input) {
  const parsed = parseCurseForgeInput(input.query ?? input.input ?? "");
  const loader = normalizeLoader(input.loader ?? "");
  const query = (input.query ?? input.input ?? "").trim();
  const normalizedName = normalizeText(query);

  return {
    query,
    inputType: input.inputType && input.inputType !== "auto" ? input.inputType : parsed.inputType ?? "name",
    normalizedName,
    querySlug: slugify(query),
    minecraftVersion: input.minecraftVersion ?? input.mcVersion ?? null,
    loader,
    loaderName: loaderDisplayName(loader),
    loaderTypeId: loader ? LOADER_ALIASES[loader].id : null,
    hintSlug: input.hintSlug ? input.hintSlug.toLowerCase() : parsed.slug ?? null,
    hintProjectId: input.hintProjectId ? Number(input.hintProjectId) : parsed.projectId ?? null,
    hintAuthor: input.hintAuthor ?? null,
    inputUrl: parsed.url ?? null,
    hintedFileId: input.fileId ? Number(input.fileId) : parsed.fileId ?? null,
    mode: input.mode ?? "strict",
  };
}

export function buildUrls(input) {
  const normalized = normalizeQuery(input);
  const params = new URLSearchParams({
    page: "1",
    pageSize: "20",
    sortBy: "relevancy",
    class: "mc-mods",
    search: normalized.query,
  });
  if (normalized.minecraftVersion) {
    params.set("version", normalized.minecraftVersion);
  }
  if (normalized.loaderTypeId) {
    params.set("gameVersionTypeId", String(normalized.loaderTypeId));
  }

  const searchUrl = `${CURSEFORGE_BASE_URL}/minecraft/search?${params.toString()}`;
  const projectSlug = normalized.hintSlug;
  const projectUrl = projectSlug ? `${CURSEFORGE_BASE_URL}/minecraft/mc-mods/${projectSlug}` : null;

  let filesUrl = null;
  if (projectSlug && normalized.minecraftVersion && normalized.loaderTypeId) {
    filesUrl = `${projectUrl}/files/all?page=1&pageSize=20&version=${encodeURIComponent(normalized.minecraftVersion)}&gameVersionTypeId=${normalized.loaderTypeId}&showAlphaFiles=show`;
  } else if (projectSlug) {
    filesUrl = `${projectUrl}/files/all?page=1&pageSize=20&showAlphaFiles=show`;
  }

  const fileUrl = projectSlug && normalized.hintedFileId ? `${projectUrl}/files/${normalized.hintedFileId}` : null;

  return {
    normalized,
    searchUrl,
    projectUrl,
    filesUrl,
    fileUrl,
  };
}

function scoreCandidate(candidate, normalized) {
  let score = 0;
  const reasons = [];
  const querySlug = normalized.hintSlug ?? normalized.querySlug;
  const normalizedTitle = normalizeText(candidate.title);
  const normalizedAuthor = normalizeText(candidate.author);

  if (querySlug && candidate.slug === querySlug) {
    score += 120;
    reasons.push("exact_slug_match");
  }
  if (normalized.normalizedName && normalizedTitle === normalized.normalizedName) {
    score += 110;
    reasons.push("exact_title_match");
  }
  if (normalized.hintAuthor && normalizedAuthor === normalizeText(normalized.hintAuthor)) {
    score += 25;
    reasons.push("exact_author_match");
  }

  const queryTokens = unique(normalized.normalizedName.split(" "));
  const titleTokens = new Set(normalizedTitle.split(" "));
  const slugTokens = new Set(candidate.slug.split("-"));
  const tokenMatches = queryTokens.filter((token) => titleTokens.has(token) || slugTokens.has(token));
  if (queryTokens.length) {
    const coverage = tokenMatches.length / queryTokens.length;
    score += Math.round(coverage * 40);
    if (coverage === 1) {
      reasons.push("full_token_coverage");
    }
  }

  return { score, reasons };
}

export function parseSearchResults(input) {
  const normalized = normalizeQuery(input);
  const text = input.text ?? "";
  const cardStartRegex = /\[\]\(\/minecraft\/mc-mods\/([^\s)]+)\s+"Go to .*? Project Page"\)/g;
  const starts = [...text.matchAll(cardStartRegex)];
  const candidates = [];

  for (let index = 0; index < starts.length; index += 1) {
    const match = starts[index];
    const slug = match[1];
    const start = match.index ?? 0;
    const end = starts[index + 1]?.index ?? text.length;
    const chunk = text.slice(start, end);
    const titleRegex = new RegExp(`\\[([^\\]]+)\\]\\(/minecraft/mc-mods/${escapeRegex(slug)}\\s+"Go to .*? Project Page"\\)`);
    const title = chunk.match(titleRegex)?.[1] ?? slug;
    const author = chunk.match(/By\[([^\]]+)\]\(/)?.[1] ?? null;
    const descriptionLine = meaningfulLines(chunk).find((line) => ![title, author, "Download", "Install"].includes(line) && !/^\d/.test(line));
    const { score, reasons } = scoreCandidate({ slug, title, author }, normalized);

    candidates.push({
      slug,
      title,
      author,
      description: descriptionLine ?? null,
      url: `${CURSEFORGE_BASE_URL}/minecraft/mc-mods/${slug}`,
      score,
      reasons,
    });
  }

  const deduped = Object.values(
    candidates.reduce((acc, candidate) => {
      if (!acc[candidate.slug] || acc[candidate.slug].score < candidate.score) {
        acc[candidate.slug] = candidate;
      }
      return acc;
    }, {}),
  ).sort((left, right) => right.score - left.score || left.slug.localeCompare(right.slug));

  const top = deduped[0] ?? null;
  const second = deduped[1] ?? null;
  const strongExact = top && (top.reasons.includes("exact_slug_match") || top.reasons.includes("exact_title_match"));
  const clearMargin = top && (!second || top.score - second.score >= 20);
  const verified = Boolean(top && strongExact && clearMargin);

  return {
    status: verified ? "resolved" : deduped.length ? "needs_confirmation" : "not_found",
    normalized,
    selectedProject: verified
      ? {
          slug: top.slug,
          title: top.title,
          author: top.author,
          url: top.url,
          verificationReasons: top.reasons,
        }
      : null,
    candidates: deduped.slice(0, 5),
    reason: verified
      ? "strong search candidate verified"
      : deduped.length
        ? "unexpected or ambiguous search results"
        : "no search candidates found",
  };
}

function extractVersionTokens(block) {
  return unique(
    [...block.matchAll(/\b\d+\.\d+(?:\.\d+)?(?:-[A-Za-z0-9.-]+)?\b/g)].map((match) => match[0]),
  );
}

function extractLoaders(block) {
  const lowered = block.toLowerCase();
  const ordered = Object.entries(LOADER_ALIASES).sort((left, right) => right[1].name.length - left[1].name.length);
  return ordered
    .filter(([, value]) => new RegExp(`(^|[^a-z])${escapeRegex(value.name.toLowerCase())}([^a-z]|$)`, "i").test(lowered))
    .map(([loader]) => loader);
}

function parseReleaseType(lines) {
  return lines.find((line) => ["R", "B", "A"].includes(line)) ?? null;
}

export function selectFileFromFilesPage(input) {
  const normalized = normalizeQuery(input);
  const text = input.text ?? "";
  const tableIndex = Math.max(text.lastIndexOf("Type\n\nName\n\nUploaded"), text.lastIndexOf("Type\r\n\r\nName\r\n\r\nUploaded"));
  const section = tableIndex >= 0 ? text.slice(tableIndex) : text;
  const rowRegex = /\[(?<body>[\s\S]*?)\]\(\/minecraft\/mc-mods\/(?<slug>[^/]+)\/files\/(?<fileId>\d+)\)/g;
  const rows = [];

  for (const match of section.matchAll(rowRegex)) {
    const body = match.groups?.body ?? "";
    const lines = meaningfulLines(body);
    const releaseType = parseReleaseType(lines);
    if (!releaseType) {
      continue;
    }
    const slug = match.groups?.slug ?? null;
    const fileId = Number(match.groups?.fileId ?? 0);
    const displayName = lines.find((line) => line !== releaseType) ?? `file-${fileId}`;
    const loaders = extractLoaders(body);
    const versions = extractVersionTokens(body);

    rows.push({
      slug,
      fileId,
      displayName,
      releaseType,
      versions,
      loaders,
      url: `${CURSEFORGE_BASE_URL}/minecraft/mc-mods/${slug}/files/${fileId}`,
      priority: RELEASE_PRIORITY[releaseType] ?? 0,
    });
  }

  const filtered = rows.filter((row) => {
    const versionMatch = normalized.minecraftVersion ? row.versions.includes(normalized.minecraftVersion) : true;
    const loaderMatch = normalized.loader ? row.loaders.includes(normalized.loader) : true;
    const slugMatch = normalized.hintSlug ? row.slug === normalized.hintSlug : true;
    return versionMatch && loaderMatch && slugMatch;
  });

  const bestPriority = Math.max(0, ...filtered.map((row) => row.priority));
  const selected = filtered.find((row) => row.priority === bestPriority) ?? null;

  return {
    status: selected ? "resolved" : filtered.length ? "needs_confirmation" : "not_found",
    normalized,
    selectedFile: selected,
    candidates: filtered.slice(0, 20),
    reason: selected ? `selected first ${selected.releaseType} row in filtered files page` : "no exact file rows matched expected version and loader",
  };
}

function parseProjectDetails(text) {
  const title = text.match(/^#\s+(.+)$/m)?.[1] ?? null;
  const author = text.match(/By\[([^\]]+)\]\(/)?.[1] ?? null;
  const projectId = text.match(/Project ID\s+(\d+)/)?.[1];
  return {
    title,
    author,
    projectId: projectId ? Number(projectId) : null,
  };
}

export function verifyFilePage(input) {
  const normalized = normalizeQuery(input);
  const text = input.text ?? "";
  const project = parseProjectDetails(text);
  const fileTitle = text.match(/^##\s+(.+)$/m)?.[1] ?? null;
  const fileNameMatch = text.match(/### File Name\s+\s*([A-Za-z0-9._+\-\[\] ()]+\.jar)/m);
  const fileName = fileNameMatch?.[1] ?? null;
  const mavenMatch = text.match(/curse\.maven:[^:]+:(\d+)/);
  const supportedVersionsBlock = text.match(/### Supported Versions([\s\S]*?)(?:\n###|\n##|$)/);
  const supportedVersions = supportedVersionsBlock ? extractVersionTokens(supportedVersionsBlock[1]) : [];
  const loaders = extractLoaders(text);
  const fileId = input.expectedFileId ? Number(input.expectedFileId) : mavenMatch ? Number(mavenMatch[1]) : null;
  const checks = [];

  if (normalized.hintProjectId) {
    checks.push({
      check: "project_id",
      passed: project.projectId === normalized.hintProjectId,
      expected: normalized.hintProjectId,
      actual: project.projectId,
    });
  }
  if (normalized.hintSlug) {
    const titleSlug = project.title ? slugify(project.title) : null;
    checks.push({
      check: "slug_present",
      passed: text.toLowerCase().includes(`/minecraft/mc-mods/${normalized.hintSlug}`) || titleSlug === normalized.hintSlug,
      expected: normalized.hintSlug,
      actual: titleSlug,
    });
  }
  if (normalized.normalizedName) {
    checks.push({
      check: "project_title",
      passed: project.title ? normalizeText(project.title) === normalized.normalizedName : false,
      expected: normalized.normalizedName,
      actual: project.title ? normalizeText(project.title) : null,
    });
  }
  if (normalized.hintAuthor) {
    checks.push({
      check: "author",
      passed: project.author ? normalizeText(project.author) === normalizeText(normalized.hintAuthor) : false,
      expected: normalizeText(normalized.hintAuthor),
      actual: project.author ? normalizeText(project.author) : null,
    });
  }
  if (normalized.minecraftVersion) {
    checks.push({
      check: "minecraft_version",
      passed: supportedVersions.includes(normalized.minecraftVersion),
      expected: normalized.minecraftVersion,
      actual: supportedVersions,
    });
  }
  if (normalized.loader) {
    checks.push({
      check: "loader",
      passed: loaders.includes(normalized.loader),
      expected: normalized.loader,
      actual: loaders,
    });
  }
  if (input.expectedFileId) {
    checks.push({
      check: "file_id",
      passed: fileId === Number(input.expectedFileId),
      expected: Number(input.expectedFileId),
      actual: fileId,
    });
  }

  const failedChecks = checks.filter((check) => !check.passed);
  return {
    status: failedChecks.length ? "needs_confirmation" : "resolved",
    project: {
      title: project.title,
      author: project.author,
      projectId: project.projectId,
    },
    file: {
      fileId,
      fileTitle,
      fileName,
      supportedVersions,
      loaders,
    },
    checks,
    failedChecks,
    reason: failedChecks.length ? "file page verification found unexpected metadata" : "file page verified",
  };
}

function cachePath(worktree) {
  return path.join(worktree, ".opencode", "cache", "curseforge-helper-cache.json");
}

async function readCache(worktree) {
  const filePath = cachePath(worktree);
  try {
    const raw = await fs.readFile(filePath, "utf8");
    return JSON.parse(raw);
  } catch {
    return { projectAliases: {}, fileSelections: {} };
  }
}

async function writeCache(worktree, cache) {
  const filePath = cachePath(worktree);
  await fs.mkdir(path.dirname(filePath), { recursive: true });
  await fs.writeFile(filePath, `${JSON.stringify(cache, null, 2)}\n`, "utf8");
}

function projectAliasKeys(query, project) {
  const keys = [];
  if (query) {
    keys.push(normalizeText(query));
    keys.push(slugify(query));
  }
  if (project?.slug) {
    keys.push(project.slug.toLowerCase());
  }
  if (project?.title) {
    keys.push(normalizeText(project.title));
  }
  if (project?.projectId) {
    keys.push(String(project.projectId));
  }
  return unique(keys);
}

function fileSelectionKey(project, minecraftVersion, loader) {
  return [project?.projectId ?? project?.slug ?? "unknown", minecraftVersion ?? "any", loader ?? "any"].join("::");
}

export async function lookupCache(input, worktree) {
  const normalized = normalizeQuery(input);
  const cache = await readCache(worktree);
  const projectKeys = unique([
    normalizeText(normalized.query),
    normalized.querySlug,
    normalized.hintSlug,
    normalized.hintProjectId ? String(normalized.hintProjectId) : null,
  ]);

  const project = projectKeys.map((key) => cache.projectAliases[key]).find(Boolean) ?? null;
  const file = project
    ? cache.fileSelections[fileSelectionKey(project, normalized.minecraftVersion, normalized.loader)] ?? null
    : null;

  return {
    status: project ? "resolved" : "not_found",
    project,
    file,
    projectKeys,
  };
}

export async function storeResolution(input, worktree) {
  const normalized = normalizeQuery(input);
  const cache = await readCache(worktree);
  const project = input.project ?? null;
  const file = input.file ?? null;

  if (project) {
    for (const key of projectAliasKeys(normalized.query, project)) {
      cache.projectAliases[key] = project;
    }
  }
  if (project && file) {
    cache.fileSelections[fileSelectionKey(project, normalized.minecraftVersion, normalized.loader)] = file;
  }

  await writeCache(worktree, cache);
  return {
    status: "stored",
    projectStored: Boolean(project),
    fileStored: Boolean(project && file),
  };
}

export async function executeAction(input, worktree) {
  switch (input.action) {
    case "build_urls":
      return buildUrls(input);
    case "parse_search":
      return parseSearchResults(input);
    case "select_file":
      return selectFileFromFilesPage(input);
    case "verify_file":
      return verifyFilePage(input);
    case "lookup_cache":
      return lookupCache(input, worktree);
    case "store_resolution":
      return storeResolution(input, worktree);
    default:
      return {
        status: "error",
        reason: `Unknown action: ${input.action}`,
      };
  }
}
