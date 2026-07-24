import {
  Add01Icon,
  AlertCircleIcon,
  CheckmarkCircle02Icon,
  Delete02Icon,
  Loading03Icon,
  PencilEdit01Icon,
  RefreshIcon,
  UserGroupIcon,
} from "@hugeicons/core-free-icons"
import { HugeiconsIcon } from "@hugeicons/react"
import {
  useEffect,
  useRef,
  useState,
  type MouseEvent,
} from "react"

import {
  Alert,
  AlertAction,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  createTeam,
  deleteTeam,
  listTeams,
  updateTeam,
} from "@/features/administration/api/teams-api"
import { DeleteTeamDialog } from "@/features/administration/components/DeleteTeamDialog"
import { TeamFormDialog } from "@/features/administration/components/TeamFormDialog"
import type { Team } from "@/features/administration/model/team.types"
import { ApiError } from "@/shared/api/api-error"

type LoadState = "loading" | "ready" | "error"

type TeamFormState =
  | {
      mode: "create"
    }
  | {
      mode: "edit"
      team: Team
    }

function sortTeams(teams: Team[]) {
  return [...teams].sort(
    (first, second) =>
      first.name.localeCompare(second.name, undefined, {
        sensitivity: "base",
      }) || first.id - second.id,
  )
}

function getRequestErrorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) {
    return error.message
  }

  return fallback
}

export function TeamManagementSection() {
  const [teams, setTeams] = useState<Team[]>([])
  const [loadState, setLoadState] = useState<LoadState>("loading")
  const [loadError, setLoadError] = useState<string>()
  const [isRetrying, setIsRetrying] = useState(false)
  const [successMessage, setSuccessMessage] = useState<string>()
  const [teamForm, setTeamForm] = useState<TeamFormState>()
  const [isTeamFormOpen, setIsTeamFormOpen] = useState(false)
  const [teamToDelete, setTeamToDelete] = useState<Team>()
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const sectionTitleRef = useRef<HTMLHeadingElement>(null)
  const createButtonRef = useRef<HTMLButtonElement>(null)
  const formFinalFocusRef = useRef<HTMLElement | null>(null)
  const deleteFinalFocusRef = useRef<HTMLElement | null>(null)

  useEffect(() => {
    const controller = new AbortController()

    async function loadInitialTeams() {
      try {
        const response = await listTeams(controller.signal)
        setTeams(sortTeams(response))
        setLoadError(undefined)
        setLoadState("ready")
      } catch (error) {
        if (controller.signal.aborted) {
          return
        }

        setLoadError(
          getRequestErrorMessage(
            error,
            "Teams are unavailable. Try loading them again.",
          ),
        )
        setLoadState("error")
      }
    }

    void loadInitialTeams()

    return () => controller.abort()
  }, [])

  useEffect(() => {
    if (!successMessage) {
      return
    }

    const timeout = window.setTimeout(
      () => setSuccessMessage(undefined),
      3500,
    )

    return () => window.clearTimeout(timeout)
  }, [successMessage])

  async function handleRetry() {
    if (isRetrying) {
      return
    }

    setIsRetrying(true)

    try {
      const response = await listTeams()
      setTeams(sortTeams(response))
      setLoadError(undefined)
      setLoadState("ready")
      requestAnimationFrame(() => sectionTitleRef.current?.focus())
    } catch (error) {
      setLoadError(
        getRequestErrorMessage(
          error,
          "Teams are unavailable. Try loading them again.",
        ),
      )
    } finally {
      setIsRetrying(false)
    }
  }

  function openCreateDialog(event: MouseEvent<HTMLButtonElement>) {
    formFinalFocusRef.current = event.currentTarget
    setTeamForm({ mode: "create" })
    setIsTeamFormOpen(true)
  }

  function openEditDialog(
    event: MouseEvent<HTMLButtonElement>,
    team: Team,
  ) {
    formFinalFocusRef.current = event.currentTarget
    setTeamForm({ mode: "edit", team })
    setIsTeamFormOpen(true)
  }

  function openDeleteDialog(
    event: MouseEvent<HTMLButtonElement>,
    team: Team,
  ) {
    deleteFinalFocusRef.current = event.currentTarget
    setTeamToDelete(team)
    setIsDeleteDialogOpen(true)
  }

  async function handleCreate(name: string) {
    setSuccessMessage(undefined)
    const createdTeam = await createTeam({ name })
    formFinalFocusRef.current = createButtonRef.current
    setTeams((current) => sortTeams([...current, createdTeam]))
    setSuccessMessage("Team created.")
  }

  async function handleUpdate(team: Team, name: string) {
    setSuccessMessage(undefined)
    const updatedTeam = await updateTeam(team.id, { name })
    setTeams((current) =>
      sortTeams(
        current.map((currentTeam) =>
          currentTeam.id === updatedTeam.id ? updatedTeam : currentTeam,
        ),
      ),
    )
    setSuccessMessage("Team updated.")
  }

  async function handleDelete(team: Team) {
    setSuccessMessage(undefined)
    await deleteTeam(team.id)
    deleteFinalFocusRef.current = sectionTitleRef.current
    setTeams((current) =>
      current.filter((currentTeam) => currentTeam.id !== team.id),
    )
    setSuccessMessage("Team deleted.")
  }

  const createDisabled = loadState !== "ready"

  return (
    <>
      <Card className="scroll-mt-6" id="teams">
        <CardHeader>
          <CardTitle>
            <h2 ref={sectionTitleRef} tabIndex={-1}>
              Teams
            </h2>
          </CardTitle>
          <CardDescription>
            Maintain the groups that own incidents and coordinate responders.
          </CardDescription>
          <CardAction>
            <Button
              disabled={createDisabled}
              onClick={openCreateDialog}
              ref={createButtonRef}
              type="button"
            >
              <HugeiconsIcon
                data-icon="inline-start"
                icon={Add01Icon}
                strokeWidth={2}
              />
              Create team
            </Button>
          </CardAction>
        </CardHeader>

        <CardContent className="flex flex-col gap-4">
          {successMessage ? (
            <Alert role="status">
              <HugeiconsIcon icon={CheckmarkCircle02Icon} strokeWidth={2} />
              <AlertTitle>{successMessage}</AlertTitle>
            </Alert>
          ) : null}

          {loadState === "loading" ? <TeamListSkeleton /> : null}

          {loadState === "error" ? (
            <Alert variant="destructive">
              <HugeiconsIcon icon={AlertCircleIcon} strokeWidth={2} />
              <AlertTitle>Unable to load teams</AlertTitle>
              <AlertDescription>{loadError}</AlertDescription>
              <AlertAction>
                <Button
                  disabled={isRetrying}
                  onClick={handleRetry}
                  size="sm"
                  type="button"
                  variant="outline"
                >
                  {isRetrying ? (
                    <HugeiconsIcon
                      className="animate-spin"
                      data-icon="inline-start"
                      icon={Loading03Icon}
                      strokeWidth={2}
                    />
                  ) : (
                    <HugeiconsIcon
                      data-icon="inline-start"
                      icon={RefreshIcon}
                      strokeWidth={2}
                    />
                  )}
                  {isRetrying ? "Retrying…" : "Retry"}
                </Button>
              </AlertAction>
            </Alert>
          ) : null}

          {loadState === "ready" && teams.length === 0 ? (
            <Empty className="border">
              <EmptyHeader>
                <EmptyMedia variant="icon">
                  <HugeiconsIcon icon={UserGroupIcon} strokeWidth={2} />
                </EmptyMedia>
                <EmptyTitle>No teams yet</EmptyTitle>
                <EmptyDescription>
                  Create the first team before assigning incident ownership.
                </EmptyDescription>
              </EmptyHeader>
              <EmptyContent>
                <Button onClick={openCreateDialog} type="button">
                  <HugeiconsIcon
                    data-icon="inline-start"
                    icon={Add01Icon}
                    strokeWidth={2}
                  />
                  Create team
                </Button>
              </EmptyContent>
            </Empty>
          ) : null}

          {loadState === "ready" && teams.length > 0 ? (
            <div className="rounded-lg border">
              <Table>
                <TableCaption className="sr-only">
                  Teams in the administrative catalog
                </TableCaption>
                <TableHeader>
                  <TableRow>
                    <TableHead scope="col">Team</TableHead>
                    <TableHead scope="col">Identifier</TableHead>
                    <TableHead className="text-right" scope="col">
                      Actions
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {teams.map((team) => (
                    <TableRow key={team.id}>
                      <TableCell className="max-w-xs whitespace-normal font-medium">
                        {team.name}
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline">#{team.id}</Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex justify-end gap-2">
                          <Button
                            aria-label={`Edit ${team.name}`}
                            onClick={(event) =>
                              openEditDialog(event, team)
                            }
                            size="sm"
                            type="button"
                            variant="outline"
                          >
                            <HugeiconsIcon
                              data-icon="inline-start"
                              icon={PencilEdit01Icon}
                              strokeWidth={2}
                            />
                            Edit
                          </Button>
                          <Button
                            aria-label={`Delete ${team.name}`}
                            onClick={(event) =>
                              openDeleteDialog(event, team)
                            }
                            size="sm"
                            type="button"
                            variant="destructive"
                          >
                            <HugeiconsIcon
                              data-icon="inline-start"
                              icon={Delete02Icon}
                              strokeWidth={2}
                            />
                            Delete
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          ) : null}
        </CardContent>

        {loadState === "ready" ? (
          <CardFooter>
            {teams.length} {teams.length === 1 ? "team" : "teams"}
          </CardFooter>
        ) : null}
      </Card>

      {teamForm?.mode === "create" ? (
        <TeamFormDialog
          finalFocusRef={formFinalFocusRef}
          mode="create"
          onOpenChange={setIsTeamFormOpen}
          onSubmit={handleCreate}
          open={isTeamFormOpen}
        />
      ) : null}

      {teamForm?.mode === "edit" ? (
        <TeamFormDialog
          finalFocusRef={formFinalFocusRef}
          mode="edit"
          onOpenChange={setIsTeamFormOpen}
          onSubmit={(name) => handleUpdate(teamForm.team, name)}
          open={isTeamFormOpen}
          team={teamForm.team}
        />
      ) : null}

      {teamToDelete ? (
        <DeleteTeamDialog
          finalFocusRef={deleteFinalFocusRef}
          onDelete={handleDelete}
          onOpenChange={setIsDeleteDialogOpen}
          open={isDeleteDialogOpen}
          team={teamToDelete}
        />
      ) : null}
    </>
  )
}

function TeamListSkeleton() {
  return (
    <div
      aria-label="Loading teams"
      aria-live="polite"
      className="flex flex-col gap-3"
      role="status"
    >
      <span className="sr-only">Loading teams…</span>
      <Skeleton className="h-8 w-full" />
      <Skeleton className="h-12 w-full" />
      <Skeleton className="h-12 w-full" />
      <Skeleton className="h-12 w-full" />
    </div>
  )
}
