export const TEAM_NAME_MAX_LENGTH = 100

export function normalizeTeamName(name: string) {
  return name.trim()
}

export function validateTeamName(name: string) {
  const normalizedName = normalizeTeamName(name)

  if (!normalizedName) {
    return "Enter a team name."
  }

  if (normalizedName.length > TEAM_NAME_MAX_LENGTH) {
    return `Team name must not exceed ${TEAM_NAME_MAX_LENGTH} characters.`
  }
}
