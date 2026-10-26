#!/usr/bin/env bash
set -euo pipefail
umask 077

repo="mookoan/tsunext"
key_dir="$HOME/.local/share/tsunext"
key_file="$key_dir/signingkey.jks"
key_alias="extensions"

for tool in gh keytool base64; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "Missing required command: $tool" >&2
    exit 1
  fi
done

login="$(gh api user --jq .login)"
if [[ "$login" != "mookoan" ]]; then
  echo "GitHub CLI is authenticated as $login, not mookoan. Run 'gh auth switch --user mookoan' or 'gh auth login'." >&2
  exit 1
fi

mkdir -p "$key_dir"
if [[ ! -f "$key_file" ]]; then
  if gh secret list --repo "$repo" --json name --jq '.[].name' | grep -qx SIGNING_KEY; then
    echo "A release signing secret already exists, but $key_file is missing. Restore your original keystore to avoid breaking updates." >&2
    exit 1
  fi
fi

IFS= read -r -s -p "Release signing password (6+ characters): " password
printf '\n'
if (( ${#password} < 6 )); then
  echo "Signing password must have at least six characters." >&2
  exit 1
fi
trap 'unset password confirmation' EXIT

if [[ ! -f "$key_file" ]]; then
  IFS= read -r -s -p "Repeat release signing password: " confirmation
  printf '\n'
  if [[ "$password" != "$confirmation" ]]; then
    echo "Passwords did not match." >&2
    exit 1
  fi
  unset confirmation
  KEY_STORE_PASSWORD="$password" KEY_PASSWORD="$password" keytool -genkeypair \
    -keystore "$key_file" -storetype JKS -alias "$key_alias" \
    -keyalg RSA -keysize 4096 -validity 10000 \
    -dname "CN=TsunExt Extensions, O=mookoan" \
    -storepass:env KEY_STORE_PASSWORD -keypass:env KEY_PASSWORD
fi

KEY_STORE_PASSWORD="$password" keytool -list \
  -keystore "$key_file" -alias "$key_alias" \
  -storepass:env KEY_STORE_PASSWORD >/dev/null

base64 < "$key_file" | tr -d '\n' | gh secret set SIGNING_KEY --repo "$repo"
printf '%s' "$password" | gh secret set KEY_STORE_PASSWORD --repo "$repo"
printf '%s' "$password" | gh secret set KEY_PASSWORD --repo "$repo"
printf '%s' "$key_alias" | gh secret set ALIAS --repo "$repo"
unset password

gh workflow run build.yml --repo "$repo" --ref main -f publish=true

printf '\nPublishing workflow dispatched. Follow its status at:\n'
printf '  https://github.com/%s/actions/workflows/build.yml\n' "$repo"
printf 'Back up the keystore at %s and its password securely. Do not regenerate it for updates.\n' "$key_file"
printf 'Tsundoku repository index (after release succeeds):\n'
printf '  https://github.com/%s/releases/latest/download/index.pb\n' "$repo"
