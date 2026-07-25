import {
  AlertTriangleIcon,
  Clock3Icon,
  PencilIcon,
  RefreshCwIcon,
  UserRoundIcon,
} from "lucide-react"
import { useEffect, useRef, useState } from "react"

import {
  Alert,
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
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import {
  getIncident,
  updateIncident,
} from "@/features/incidents/api/incidents-api"
import { IncidentFormDialog } from "@/features/incidents/components/IncidentFormDialog"
import type {
  IncidentDetail,
  IncidentEventKind,
  IncidentPriority,
  IncidentRequest,
  IncidentStatus,
} from "@/features/incidents/model/incident.types"
import { ApiError } from "@/shared/api/api-error"

type LoadState = "loading" | "ready" | "error"

const statusLabels: Record<IncidentStatus, string> = {
  OPEN: "Open",
  ACKNOWLEDGED: "Acknowledged",
  INVESTIGATING: "Investigating",
  RESOLVED: "Resolved",
  CLOSED: "Closed",
}

const eventLabels: Record<IncidentEventKind, string> = {
  CREATED: "Incident created",
  STATUS_CHANGED: "Status changed",
  NOTE_ADDED: "Note added",
  ESCALATED: "Incident escalated",
}

function priorityVariant(priority: IncidentPriority) {
  return priority === "SEV1" ? ("destructive" as const) : ("outline" as const)
}

function statusVariant(status: IncidentStatus) {
  return status === "OPEN" || status === "INVESTIGATING"
    ? ("default" as const)
    : ("secondary" as const)
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value))
}

interface IncidentDetailPanelProps {
  incidentId: number
  initialIncident?: IncidentDetail
  onUpdated: (incident: IncidentDetail) => void
}

export function IncidentDetailPanel({
  incidentId,
  initialIncident,
  onUpdated,
}: IncidentDetailPanelProps) {
  const matchingInitialIncident =
    initialIncident?.id === incidentId ? initialIncident : undefined
  const [incident, setIncident] = useState<IncidentDetail | undefined>(
    matchingInitialIncident,
  )
  const [loadState, setLoadState] = useState<LoadState>(
    matchingInitialIncident ? "ready" : "loading",
  )
  const [loadError, setLoadError] = useState<string>()
  const [loadVersion, setLoadVersion] = useState(0)
  const [isEditing, setIsEditing] = useState(false)
  const editButtonRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (matchingInitialIncident) {
      setIncident(matchingInitialIncident)
      setLoadError(undefined)
      setLoadState("ready")
      return
    }

    const controller = new AbortController()

    async function loadDetail() {
      setIncident(undefined)
      setLoadState("loading")
      setLoadError(undefined)

      try {
        const response = await getIncident(incidentId, controller.signal)
        setIncident(response)
        setLoadState("ready")
      } catch (error) {
        if (!controller.signal.aborted) {
          setLoadError(
            error instanceof ApiError
              ? error.message
              : "The incident detail is unavailable. Try loading it again.",
          )
          setLoadState("error")
        }
      }
    }

    void loadDetail()
    return () => controller.abort()
  }, [incidentId, loadVersion, matchingInitialIncident])

  async function submitUpdate(request: IncidentRequest) {
    return updateIncident(incidentId, request)
  }

  function acceptUpdate(updated: IncidentDetail) {
    setIncident(updated)
    setLoadState("ready")
    onUpdated(updated)
  }

  if (loadState === "loading") {
    return (
      <Card
        aria-label="Loading incident detail"
        aria-live="polite"
        role="status"
      >
        <CardContent className="space-y-3">
          <Skeleton className="h-5 w-48" />
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-16 w-full" />
        </CardContent>
      </Card>
    )
  }

  if (loadState === "error" || !incident) {
    return (
      <Alert variant="destructive">
        <AlertTriangleIcon />
        <AlertTitle>Unable to load incident detail</AlertTitle>
        <AlertDescription>
          <p>{loadError}</p>
          <Button
            className="mt-3"
            onClick={() => setLoadVersion((current) => current + 1)}
            size="sm"
            variant="outline"
          >
            <RefreshCwIcon />
            Retry
          </Button>
        </AlertDescription>
      </Alert>
    )
  }

  return (
    <>
      <Card aria-labelledby={`incident-${incident.id}-title`}>
        <CardHeader className="border-b">
          <CardTitle>
            <div className="flex flex-wrap items-center gap-2">
              <span className="font-mono text-xs text-muted-foreground">
                {incident.referenceCode}
              </span>
              <Badge variant={priorityVariant(incident.priority)}>
                {incident.priority}
              </Badge>
              <Badge variant={statusVariant(incident.status)}>
                {statusLabels[incident.status]}
              </Badge>
            </div>
            <h2
              className="mt-2 text-xl font-semibold tracking-tight"
              id={`incident-${incident.id}-title`}
            >
              {incident.title}
            </h2>
          </CardTitle>
          <CardDescription>
            Reported for {incident.managedService.name}
          </CardDescription>
          <CardAction>
            <Button
              onClick={() => setIsEditing(true)}
              ref={editButtonRef}
              size="sm"
              variant="outline"
            >
              <PencilIcon />
              Edit incident
            </Button>
          </CardAction>
        </CardHeader>
        <CardContent className="space-y-6">
          <section aria-labelledby={`incident-${incident.id}-description`}>
            <h3
              className="text-sm font-medium"
              id={`incident-${incident.id}-description`}
            >
              Description
            </h3>
            <p className="mt-2 whitespace-pre-wrap text-muted-foreground">
              {incident.description}
            </p>
          </section>

          <div className="grid gap-4 rounded-lg border p-4 sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <p className="text-xs font-medium text-muted-foreground">
                Service
              </p>
              <p className="mt-1 font-medium">
                {incident.managedService.name}
              </p>
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">
                Reporter
              </p>
              <p className="mt-1 font-medium">
                {incident.reporter.displayName}
              </p>
              <p className="text-xs text-muted-foreground">
                @{incident.reporter.username}
              </p>
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">
                Assignee
              </p>
              <p className="mt-1 font-medium">
                {incident.assignee?.displayName ?? "Unassigned"}
              </p>
              {incident.assignee ? (
                <p className="text-xs text-muted-foreground">
                  @{incident.assignee.username}
                </p>
              ) : null}
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">
                Last updated
              </p>
              <time
                className="mt-1 block font-medium"
                dateTime={incident.updatedAt}
              >
                {formatDateTime(incident.updatedAt)}
              </time>
              <p className="mt-1 text-xs text-muted-foreground">
                Created{" "}
                <time dateTime={incident.createdAt}>
                  {formatDateTime(incident.createdAt)}
                </time>
              </p>
            </div>
          </div>

          <Separator />

          <section aria-labelledby={`incident-${incident.id}-timeline`}>
            <div className="flex items-center gap-2">
              <Clock3Icon className="size-4 text-muted-foreground" />
              <h3
                className="text-sm font-medium"
                id={`incident-${incident.id}-timeline`}
              >
                Timeline
              </h3>
            </div>
            <ol
              aria-label="Incident timeline"
              className="mt-4 space-y-4 border-l pl-4"
            >
              {incident.timeline.map((entry) => (
                <li className="relative" key={entry.id}>
                  <span className="absolute top-1.5 -left-[1.31rem] size-2 rounded-full bg-primary ring-4 ring-card" />
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div>
                      <p className="font-medium">
                        {eventLabels[entry.kind]}
                      </p>
                      <p className="mt-1 flex items-center gap-1.5 text-sm text-muted-foreground">
                        <UserRoundIcon className="size-3.5" />
                        {entry.kind === "CREATED"
                          ? `Created by ${entry.actor.displayName}`
                          : `Recorded by ${entry.actor.displayName}`}
                      </p>
                    </div>
                    <time
                      className="text-xs text-muted-foreground"
                      dateTime={entry.occurredAt}
                    >
                      {formatDateTime(entry.occurredAt)}
                    </time>
                  </div>
                </li>
              ))}
            </ol>
          </section>
        </CardContent>
      </Card>

      <IncidentFormDialog
        finalFocusRef={editButtonRef}
        incident={incident}
        mode="edit"
        onOpenChange={setIsEditing}
        onSubmit={submitUpdate}
        onSuccess={acceptUpdate}
        open={isEditing}
      />
    </>
  )
}
