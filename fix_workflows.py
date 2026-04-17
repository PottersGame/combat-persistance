import os
import re

env_additions = """          GEMINI_API_KEY: '${{ secrets.GEMINI_API_KEY }}'
          GOOGLE_API_KEY: '${{ secrets.GOOGLE_API_KEY }}'
          GOOGLE_GENAI_USE_VERTEXAI: '${{ vars.GOOGLE_GENAI_USE_VERTEXAI }}'
          GOOGLE_GENAI_USE_GCA: '${{ vars.GOOGLE_GENAI_USE_GCA }}'"""

dir_path = ".github/workflows/"
for file_name in ["gemini-invoke.yml", "gemini-plan-execute.yml", "gemini-scheduled-triage.yml", "gemini-triage.yml"]:
    file_path = os.path.join(dir_path, file_name)
    with open(file_path, "r") as f:
        content = f.read()

    # The previous python script failed to inject the env variables if the env block didnt match `env:\n\s+GITHUB_TOKEN:`
    # Let's verify and inject properly
    if "GEMINI_API_KEY" not in content:
        # Find the env block under google-github-actions/run-gemini-cli@v0
        if "env:" in content:
            # We assume the env block exists, let's just append right after `env:`
            content = re.sub(r"env:\n", f"env:\n{env_additions}\n", content, count=1)
            with open(file_path, "w") as f:
                f.write(content)
                print(f"Fixed {file_name}")
