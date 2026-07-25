import { AlertTriangleIcon, RefreshCwIcon } from "lucide-react"
import { useEffect, useMemo, useState } from "react"
import { useSearchParams } from "react-router-dom"

import {
  Alert,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { listIncidents } from "@/features/dashboard/api/incidents-api"
import {
  incidentPriorities,
  incidentStatuses,
  type IncidentFilters,
  type IncidentPriority,
  type IncidentStatus,
  type IncidentSummary,
} from "@/features/dashboard/model/incident.types"
import { ApiError } from "@/shared/api/api-error"
import { listServiceCatalog } from "@/shared/catalogs/catalog-api"
import type { ManagedServiceCatalogItem } from "@/shared/catalogs/catalog.types"

type LoadState = "loading" | "ready" | "error"

const statusLabels: Record<IncidentStatus, string> = {
  OPEN: "Open",
  ACKNOWLEDGED: "Acknowledged",
  INVESTIGATING: "Investigating",
  RESOLVED: "Resolved",
  CLOSED: "Closed",
}

function includesValue<T extends string>(
  values: readonly T[],
  value: string | null,
): value is T {
  return value !== null && values.includes(value as T)
}

function filtersFromSearchParams(
  searchParams: URLSearchParams,
): IncidentFilters {
  const status = searchParams.get("status")
  const priority = searchParams.get("priority")
  const serviceId = Number(searchParams.get("serviceId"))

  return {
    status: includesValue(incidentStatuses, status) ? status : undefined,
    priority: includesValue(incidentPriorities, priority)
      ? priority
      : undefined,
    serviceId:
      Number.isInteger(serviceId) && serviceId > 0
        ? serviceId
        : undefined,
  }
}

function priorityVariant(priority: IncidentPriority) {
  if (priority === "SEV1") {
    return "destructive" as const
  }
  if (priority === "SEV2") {
    return "secondary" as const
  }
  return "outline" as const
}

function statusVariant(status: IncidentStatus) {
  if (status === "OPEN" || status === "INVESTIGATING") {
    return "default" as const
  }
  if (status === "ACKNOWLEDGED") {
    return "secondary" as const
  }
  return "outline" as const
}

function formatCreatedAt(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value))
}

function IncidentQueueLoading() {
  return (
    <Card
      aria-label="Loading incident queue"
      aria-live="polite"
      role="status"
    >
      <CardContent className="space-y-3">
        <Skeleton className="h-5 w-36" />
        <Skeleton className="h-16 w-full" />
        <Skeleton className="h-16 w-full" />
        <Skeleton className="h-16 w-full" />
      </CardContent>
    </Card>
  )
}

interface IncidentQueueProps {
  incidents: IncidentSummary[]
  onSelect: (incident: IncidentSummary) => void
  selectedId?: number
}

interface DashboardFiltersProps {
  filters: IncidentFilters
  onChange: (
    key: "status" | "priority" | "serviceId",
    value: string | number | undefined,
  ) => void
  onClear: () => void
  services: ManagedServiceCatalogItem[]
}

function DashboardFilters({
  filters,
  onChange,
  onClear,
  services,
}: DashboardFiltersProps) {
  const hasFilters = Boolean(
    filters.status || filters.priority || filters.serviceId,
  )

  return (
    <Card size="sm">
      <CardHeader>
        <div className="flex min-w-0 items-start justify-between gap-3">
          <div className="min-w-0">
            <CardTitle>Queue filters</CardTitle>
            <CardDescription className="mt-1">
              Narrow the incident list without losing the current view.
            </CardDescription>
          </div>
          {hasFilters ? (
            <Button onClick={onClear} size="sm" variant="ghost">
              Clear
            </Button>
          ) : null}
        </div>
      </CardHeader>
      <CardContent className="grid min-w-0 gap-3 sm:grid-cols-3">
        <div className="min-w-0 space-y-1.5">
          <label className="text-xs font-medium" htmlFor="queue-status">
            Status
          </label>
          <Select<IncidentStatus | "ALL">
            id="queue-status"
            onValueChange={(value) => {
              if (value) {
                onChange(
                  "status",
                  value === "ALL" ? undefined : value,
                )
              }
            }}
            value={filters.status ?? "ALL"}
          >
            <SelectTrigger aria-label="Status" className="w-full">
              <SelectValue>
                {(value: IncidentStatus | "ALL" | null) =>
                  value && value !== "ALL"
                    ? statusLabels[value]
                    : "All statuses"
                }
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All statuses</SelectItem>
              {incidentStatuses.map((status) => (
                <SelectItem key={status} value={status}>
                  {statusLabels[status]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="min-w-0 space-y-1.5">
          <label className="text-xs font-medium" htmlFor="queue-priority">
            Priority
          </label>
          <Select<IncidentPriority | "ALL">
            id="queue-priority"
            onValueChange={(value) => {
              if (value) {
                onChange(
                  "priority",
                  value === "ALL" ? undefined : value,
                )
              }
            }}
            value={filters.priority ?? "ALL"}
          >
            <SelectTrigger aria-label="Priority" className="w-full">
              <SelectValue>
                {(value: IncidentPriority | "ALL" | null) =>
                  value && value !== "ALL" ? value : "All priorities"
                }
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All priorities</SelectItem>
              {incidentPriorities.map((priority) => (
                <SelectItem key={priority} value={priority}>
                  {priority}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="min-w-0 space-y-1.5">
          <label className="text-xs font-medium" htmlFor="queue-service">
            Service
          </label>
          <Select<number>
            id="queue-service"
            onValueChange={(value) =>
              onChange("serviceId", value || undefined)
            }
            value={filters.serviceId ?? 0}
          >
            <SelectTrigger aria-label="Service" className="w-full">
              <SelectValue>
                {(value: number | null) =>
                  value
                    ? services.find((service) => service.id === value)
                        ?.name ?? `Service ${value}`
                    : "All services"
                }
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={0}>All services</SelectItem>
              {services.map((service) => (
                <SelectItem key={service.id} value={service.id}>
                  {service.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </CardContent>
    </Card>
  )
}

function IncidentQueue({
  incidents,
  onSelect,
  selectedId,
}: IncidentQueueProps) {
  return (
    <Card>
      <CardHeader className="border-b">
        <CardTitle>Current incidents</CardTitle>
        <CardDescription>
          {incidents.length} {incidents.length === 1 ? "incident" : "incidents"}
          , newest first
        </CardDescription>
      </CardHeader>
      <CardContent className="px-0">
        <div
          aria-hidden="true"
          className="hidden grid-cols-[minmax(0,1.7fr)_0.65fr_0.9fr_minmax(0,1fr)_minmax(0,1fr)_minmax(0,1.1fr)] gap-3 border-b bg-muted/40 px-4 py-2 text-xs font-medium text-muted-foreground lg:grid"
        >
          <span>Incident</span>
          <span>Priority</span>
          <span>Status</span>
          <span>Service</span>
          <span>Assignee</span>
          <span>Created</span>
        </div>
        <ol aria-label="Incident summaries">
          {incidents.map((incident) => {
            const selected = selectedId === incident.id
            return (
              <li className="border-b last:border-b-0" key={incident.id}>
                <button
                  aria-pressed={selected}
                  className="grid w-full min-w-0 gap-4 bg-transparent p-4 text-left text-sm transition-colors outline-none hover:bg-muted/50 focus-visible:ring-3 focus-visible:ring-inset focus-visible:ring-ring/50 data-[state=selected]:bg-muted lg:grid-cols-[minmax(0,1.7fr)_0.65fr_0.9fr_minmax(0,1fr)_minmax(0,1fr)_minmax(0,1.1fr)] lg:items-center lg:gap-3"
                  data-state={selected ? "selected" : undefined}
                  onClick={() => onSelect(incident)}
                  type="button"
                >
                  <span className="min-w-0">
                    <span className="flex min-w-0 flex-wrap items-center gap-2">
                      <span className="font-mono text-xs font-medium text-muted-foreground">
                        {incident.referenceCode}
                      </span>
                      {selected ? (
                        <Badge variant="outline">Selected</Badge>
                      ) : null}
                    </span>
                    <span className="mt-1 block truncate font-medium text-foreground">
                      {incident.title}
                    </span>
                  </span>

                  <span className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:contents">
                    <span className="min-w-0">
                      <span className="mb-1 block text-xs text-muted-foreground lg:hidden">
                        Priority
                      </span>
                      <Badge variant={priorityVariant(incident.priority)}>
                        {incident.priority}
                      </Badge>
                    </span>
                    <span className="min-w-0">
                      <span className="mb-1 block text-xs text-muted-foreground lg:hidden">
                        Status
                      </span>
                      <Badge variant={statusVariant(incident.status)}>
                        {statusLabels[incident.status]}
                      </Badge>
                    </span>
                    <span className="min-w-0">
                      <span className="mb-1 block text-xs text-muted-foreground lg:hidden">
                        Service
                      </span>
                      <span className="block truncate">
                        {incident.managedService.name}
                      </span>
                    </span>
                    <span className="min-w-0">
                      <span className="mb-1 block text-xs text-muted-foreground lg:hidden">
                        Assignee
                      </span>
                      <span className="block truncate">
                        {incident.assignee?.displayName ?? "Unassigned"}
                      </span>
                    </span>
                    <span className="min-w-0 sm:col-span-2 lg:col-span-1">
                      <span className="mb-1 block text-xs text-muted-foreground lg:hidden">
                        Created
                      </span>
                      <time
                        className="block truncate"
                        dateTime={incident.createdAt}
                      >
                        {formatCreatedAt(incident.createdAt)}
                      </time>
                    </span>
                  </span>
                </button>
              </li>
            )
          })}
        </ol>
      </CardContent>
    </Card>
  )
}

export function DashboardPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const filters = useMemo(
    () => filtersFromSearchParams(searchParams),
    [searchParams],
  )
  const [incidents, setIncidents] = useState<IncidentSummary[]>([])
  const [services, setServices] = useState<ManagedServiceCatalogItem[]>([])
  const [loadState, setLoadState] = useState<LoadState>("loading")
  const [loadError, setLoadError] = useState<string>()
  const [loadVersion, setLoadVersion] = useState(0)
  const [selectedIncidentId, setSelectedIncidentId] = useState<number>()

  useEffect(() => {
    const controller = new AbortController()

    async function loadQueue() {
      setLoadState("loading")
      setLoadError(undefined)

      try {
        const [incidentResponse, serviceResponse] = await Promise.all([
          listIncidents(filters, controller.signal),
          listServiceCatalog(controller.signal),
        ])
        setIncidents(incidentResponse)
        setServices(
          [...serviceResponse].sort(
            (first, second) =>
              first.name.localeCompare(second.name, undefined, {
                sensitivity: "base",
              }) || first.id - second.id,
          ),
        )
        setSelectedIncidentId((current) =>
          current &&
          incidentResponse.some((incident) => incident.id === current)
            ? current
            : undefined,
        )
        setLoadError(undefined)
        setLoadState("ready")
      } catch (error) {
        if (!controller.signal.aborted) {
          setLoadError(
            error instanceof ApiError
              ? error.message
              : "The incident queue is unavailable. Try loading it again.",
          )
          setLoadState("error")
        }
      }
    }

    void loadQueue()
    return () => controller.abort()
  }, [filters, loadVersion])

  function updateFilter(
    key: "status" | "priority" | "serviceId",
    value: string | number | undefined,
  ) {
    const next = new URLSearchParams(searchParams)

    if (value === undefined) {
      next.delete(key)
    } else {
      next.set(key, String(value))
    }

    setSearchParams(next, { replace: true })
  }

  function clearFilters() {
    setSearchParams(new URLSearchParams(), { replace: true })
  }

  const hasFilters = Boolean(
    filters.status || filters.priority || filters.serviceId,
  )

  return (
    <section className="min-w-0 space-y-6">
      <div>
        <p className="text-sm font-medium text-muted-foreground">
          IncidentOps
        </p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight">
          Incident queue
        </h1>
        <p className="mt-3 max-w-2xl text-muted-foreground">
          Review current response work and focus the queue with operational
          filters.
        </p>
      </div>

      <DashboardFilters
        filters={filters}
        onChange={updateFilter}
        onClear={clearFilters}
        services={services}
      />

      {loadState === "loading" ? <IncidentQueueLoading /> : null}

      {loadState === "error" ? (
        <Alert variant="destructive">
          <AlertTriangleIcon />
          <AlertTitle>Unable to load incident queue</AlertTitle>
          <AlertDescription>
            <p>{loadError}</p>
            <Button
              className="mt-3"
              onClick={() => {
                setLoadState("loading")
                setLoadVersion((current) => current + 1)
              }}
              size="sm"
              variant="outline"
            >
              <RefreshCwIcon />
              Retry
            </Button>
          </AlertDescription>
        </Alert>
      ) : null}

      {loadState === "ready" && incidents.length === 0 ? (
        <Empty className="min-h-64 border">
          <EmptyHeader>
            <EmptyTitle>
              {hasFilters
                ? "No incidents match these filters"
                : "No incidents in the queue"}
            </EmptyTitle>
            <EmptyDescription>
              {hasFilters
                ? "Adjust the filters to widen the queue."
                : "New incidents will appear here when they are reported."}
            </EmptyDescription>
          </EmptyHeader>
          {hasFilters ? (
            <EmptyContent>
              <Button onClick={clearFilters} variant="outline">
                Clear filters
              </Button>
            </EmptyContent>
          ) : null}
        </Empty>
      ) : null}

      {loadState === "ready" && incidents.length > 0 ? (
        <IncidentQueue
          incidents={incidents}
          onSelect={(incident) => setSelectedIncidentId(incident.id)}
          selectedId={selectedIncidentId}
        />
      ) : null}
    </section>
  )
}
