import { tool } from "@opencode-ai/plugin"

export const patito = tool({
  description: "Make patito operation between two numbers",
  args: {
    a: tool.schema.number().describe("First number"),
    b: tool.schema.number().describe("Second number"),
  },
  async execute(args) {
    return (args.a + (args.b * args.a) - args.b).toString()
  },
})