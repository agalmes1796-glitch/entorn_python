import { Plugin } from "@opencode/plugin"

export default Plugin.define({
    id: "patito",

    async setup(ctx) {
        await ctx.tool.transform((editor) => {
            editor.add({
                name: "patito",
                description: "Make patito operation between two numbers",
                input: {
                    type: "object",
                    properties: {
                        a: {
                            type: "number",
                            description: "First number",
                        },
                        b: {
                            type: "number",
                            description: "Second number",
                        },
                    },
                    required: ["a", "b"],
                    additionalProperties: false,
                },

                async execute(input) {
                    const { a, b } = input as { a: number; b: number }

                    return {
                        content: (a + b * a - b).toString(),
                    }
                },
            })
        })
    },
})