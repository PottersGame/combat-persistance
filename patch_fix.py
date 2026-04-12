import os

workflows_dir = '.github/workflows'
for filename in os.listdir(workflows_dir):
    if filename.endswith('.yml'):
        filepath = os.path.join(workflows_dir, filename)
        with open(filepath, 'r') as f:
            content = f.read()

        # Remove the incorrectly escaped variables
        content = content.replace("          GEMINI_API_KEY: \\'\\${{ secrets.GEMINI_API_KEY }}\\'\n", "")
        content = content.replace("          GOOGLE_API_KEY: \\'\\${{ secrets.GOOGLE_API_KEY }}\\'\n", "")
        content = content.replace("          GOOGLE_GENAI_USE_VERTEXAI: \\'\\${{ vars.GOOGLE_GENAI_USE_VERTEXAI }}\\'\n", "")
        content = content.replace("          GOOGLE_GENAI_USE_GCA: \\'\\${{ vars.GOOGLE_GENAI_USE_GCA }}\\'\n", "")

        # Inject the correct variables under `env:` directly.
        # Find where the `env:` block starts for run-gemini-cli
        # We need to look for `uses: 'google-github-actions/run-gemini-cli`

        import re

        # Simple injection using substitution
        content = re.sub(
            r"(uses: '?google-github-actions/run-gemini-cli@[^\n]+(?:\n\s*id: [^\n]+)?\n\s*env:\n)",
            r"\g<1>          GEMINI_API_KEY: '${{ secrets.GEMINI_API_KEY }}'\n          GOOGLE_API_KEY: '${{ secrets.GOOGLE_API_KEY }}'\n          GOOGLE_GENAI_USE_VERTEXAI: '${{ vars.GOOGLE_GENAI_USE_VERTEXAI }}'\n          GOOGLE_GENAI_USE_GCA: '${{ vars.GOOGLE_GENAI_USE_GCA }}'\n",
            content
        )

        with open(filepath, 'w') as f:
            f.write(content)
