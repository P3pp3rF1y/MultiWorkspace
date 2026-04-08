import { tool } from "@opencode-ai/plugin";
import { executeAction } from "./lib/curseforge-helper-core.js";

export const CurseForgeHelperPlugin = async () => {
  return {
    tool: {
      curseforge_helper: tool({
        description: "Parse CurseForge webfetch output, build CurseForge URLs, and manage a local dependency resolution cache for conservative CurseForge resolution workflows.",
        args: {
          action: tool.schema
            .enum(["build_urls", "parse_search", "select_file", "verify_file", "lookup_cache", "store_resolution"])
            .describe("Helper action to run."),
          query: tool.schema.string().optional().describe("Mod query, slug, project id, or CurseForge URL."),
          inputType: tool.schema
            .enum(["auto", "name", "slug", "url", "project-id"])
            .optional()
            .describe("Optional query type hint."),
          minecraftVersion: tool.schema.string().optional().describe("Requested Minecraft version, for example 1.21.1."),
          loader: tool.schema.string().optional().describe("Requested loader, for example neoforge, forge, fabric, or quilt."),
          mode: tool.schema.enum(["strict", "fast"]).optional().describe("Resolution mode hint."),
          hintSlug: tool.schema.string().optional().describe("Known CurseForge project slug if already verified."),
          hintProjectId: tool.schema.number().optional().describe("Known CurseForge project id if already verified."),
          hintAuthor: tool.schema.string().optional().describe("Expected primary author when verifying a project."),
          fileId: tool.schema.number().optional().describe("Known or expected CurseForge file id."),
          expectedFileId: tool.schema.number().optional().describe("Expected file id for verify_file."),
          text: tool.schema.string().optional().describe("CurseForge page content fetched with the built-in webfetch tool in markdown mode."),
          project: tool.schema.record(tool.schema.string(), tool.schema.any()).optional().describe("Project object to store in cache."),
          file: tool.schema.record(tool.schema.string(), tool.schema.any()).optional().describe("File object to store in cache."),
        },
        async execute(args, context) {
          const result = await executeAction(args, context.worktree);
          context.metadata({
            title: `curseforge_helper:${args.action}`,
            metadata: {
              action: args.action,
              status: result.status ?? "ok",
            },
          });
          return JSON.stringify(result, null, 2);
        },
      }),
    },
  };
};

export default CurseForgeHelperPlugin;
