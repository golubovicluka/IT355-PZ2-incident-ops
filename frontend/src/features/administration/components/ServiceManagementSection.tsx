import {
  Add01Icon,
  AlertCircleIcon,
  CheckmarkCircle02Icon,
  Delete02Icon,
  Loading03Icon,
  PencilEdit01Icon,
  RefreshIcon,
  ServiceIcon,
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
  createManagedService,
  deleteManagedService,
  listManagedServices,
  updateManagedService,
} from "@/features/administration/api/services-api"
import { listTeams } from "@/features/administration/api/teams-api"
import { DeleteServiceDialog } from "@/features/administration/components/DeleteServiceDialog"
import { ServiceFormDialog } from "@/features/administration/components/ServiceFormDialog"
import type {
  ManagedService,
  ManagedServiceRequest,
} from "@/features/administration/model/service.types"
import { notifyManagedServicesChanged } from "@/features/administration/model/administration.events"
import { ApiError } from "@/shared/api/api-error"
import type {
  CatalogTeam,
  Criticality,
} from "@/shared/catalogs/catalog.types"

type LoadState = "loading" | "ready" | "error"

type ServiceFormState =
  | {
      mode: "create"
    }
  | {
      mode: "edit"
      service: ManagedService
    }

const criticalityLabels: Record<Criticality, string> = {
  LOW: "Low",
  MEDIUM: "Medium",
  HIGH: "High",
  CRITICAL: "Critical",
}

function sortServices(services: ManagedService[]) {
  return [...services].sort(
    (first, second) =>
      first.name.localeCompare(second.name, undefined, {
        sensitivity: "base",
      }) || first.id - second.id,
  )
}

function sortTeams(teams: CatalogTeam[]) {
  return [...teams].sort(
    (first, second) =>
      first.name.localeCompare(second.name, undefined, {
        sensitivity: "base",
      }) || first.id - second.id,
  )
}

function getRequestErrorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback
}

function criticalityVariant(criticality: Criticality) {
  if (criticality === "CRITICAL") {
    return "destructive" as const
  }
  if (criticality === "HIGH") {
    return "secondary" as const
  }
  return "outline" as const
}

export function ServiceManagementSection() {
  const [services, setServices] = useState<ManagedService[]>([])
  const [teams, setTeams] = useState<CatalogTeam[]>([])
  const [loadState, setLoadState] = useState<LoadState>("loading")
  const [loadError, setLoadError] = useState<string>()
  const [isRetrying, setIsRetrying] = useState(false)
  const [successMessage, setSuccessMessage] = useState<string>()
  const [serviceForm, setServiceForm] = useState<ServiceFormState>()
  const [isServiceFormOpen, setIsServiceFormOpen] = useState(false)
  const [serviceToDelete, setServiceToDelete] =
    useState<ManagedService>()
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const sectionTitleRef = useRef<HTMLHeadingElement>(null)
  const createButtonRef = useRef<HTMLButtonElement>(null)
  const formFinalFocusRef = useRef<HTMLElement | null>(null)
  const deleteFinalFocusRef = useRef<HTMLElement | null>(null)

  useEffect(() => {
    const controller = new AbortController()

    async function loadInitialData() {
      try {
        const [serviceResponse, teamResponse] = await Promise.all([
          listManagedServices(controller.signal),
          listTeams(controller.signal),
        ])
        setServices(sortServices(serviceResponse))
        setTeams(sortTeams(teamResponse))
        setLoadError(undefined)
        setLoadState("ready")
      } catch (error) {
        if (controller.signal.aborted) {
          return
        }

        setLoadError(
          getRequestErrorMessage(
            error,
            "Managed services or owning teams are unavailable. Try loading them again.",
          ),
        )
        setLoadState("error")
      }
    }

    void loadInitialData()

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
      const [serviceResponse, teamResponse] = await Promise.all([
        listManagedServices(),
        listTeams(),
      ])
      setServices(sortServices(serviceResponse))
      setTeams(sortTeams(teamResponse))
      setLoadError(undefined)
      setLoadState("ready")
      requestAnimationFrame(() => sectionTitleRef.current?.focus())
    } catch (error) {
      setLoadError(
        getRequestErrorMessage(
          error,
          "Managed services or owning teams are unavailable. Try loading them again.",
        ),
      )
    } finally {
      setIsRetrying(false)
    }
  }

  function openCreateDialog(event: MouseEvent<HTMLButtonElement>) {
    formFinalFocusRef.current = event.currentTarget
    setServiceForm({ mode: "create" })
    setIsServiceFormOpen(true)
  }

  function openEditDialog(
    event: MouseEvent<HTMLButtonElement>,
    service: ManagedService,
  ) {
    formFinalFocusRef.current = event.currentTarget
    setServiceForm({ mode: "edit", service })
    setIsServiceFormOpen(true)
  }

  function openDeleteDialog(
    event: MouseEvent<HTMLButtonElement>,
    service: ManagedService,
  ) {
    deleteFinalFocusRef.current = event.currentTarget
    setServiceToDelete(service)
    setIsDeleteDialogOpen(true)
  }

  async function handleCreate(request: ManagedServiceRequest) {
    setSuccessMessage(undefined)
    const created = await createManagedService(request)
    formFinalFocusRef.current = createButtonRef.current
    setServices((current) => sortServices([...current, created]))
    notifyManagedServicesChanged()
    setSuccessMessage("Managed service created.")
  }

  async function handleUpdate(
    service: ManagedService,
    request: ManagedServiceRequest,
  ) {
    setSuccessMessage(undefined)
    const updated = await updateManagedService(service.id, request)
    setServices((current) =>
      sortServices(
        current.map((item) => (item.id === updated.id ? updated : item)),
      ),
    )
    notifyManagedServicesChanged()
    setSuccessMessage("Managed service updated.")
  }

  async function handleDelete(service: ManagedService) {
    setSuccessMessage(undefined)
    await deleteManagedService(service.id)
    deleteFinalFocusRef.current = sectionTitleRef.current
    setServices((current) =>
      current.filter((item) => item.id !== service.id),
    )
    notifyManagedServicesChanged()
    setSuccessMessage("Managed service deleted.")
  }

  const createDisabled = loadState !== "ready" || teams.length === 0

  return (
    <>
      <Card className="scroll-mt-6" id="services">
        <CardHeader>
          <CardTitle>
            <h2 ref={sectionTitleRef} tabIndex={-1}>
              Managed services
            </h2>
          </CardTitle>
          <CardDescription>
            Maintain the production services available to incident forms and
            filters.
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
              Create service
            </Button>
          </CardAction>
        </CardHeader>

        <CardContent className="flex flex-col gap-4">
          {successMessage ? (
            <Alert role="status">
              <HugeiconsIcon
                icon={CheckmarkCircle02Icon}
                strokeWidth={2}
              />
              <AlertTitle>{successMessage}</AlertTitle>
            </Alert>
          ) : null}

          {loadState === "loading" ? <ServiceListSkeleton /> : null}

          {loadState === "error" ? (
            <Alert variant="destructive">
              <HugeiconsIcon icon={AlertCircleIcon} strokeWidth={2} />
              <AlertTitle>Unable to load managed services</AlertTitle>
              <AlertDescription>{loadError}</AlertDescription>
              <AlertAction>
                <Button
                  disabled={isRetrying}
                  onClick={handleRetry}
                  size="sm"
                  type="button"
                  variant="outline"
                >
                  <HugeiconsIcon
                    className={isRetrying ? "animate-spin" : undefined}
                    data-icon="inline-start"
                    icon={isRetrying ? Loading03Icon : RefreshIcon}
                    strokeWidth={2}
                  />
                  {isRetrying ? "Retrying…" : "Retry"}
                </Button>
              </AlertAction>
            </Alert>
          ) : null}

          {loadState === "ready" && teams.length === 0 ? (
            <Alert>
              <HugeiconsIcon icon={AlertCircleIcon} strokeWidth={2} />
              <AlertTitle>Create an owning team first</AlertTitle>
              <AlertDescription>
                Managed services require a valid owning team. Add one in the
                Teams section before creating a service.
              </AlertDescription>
            </Alert>
          ) : null}

          {loadState === "ready" && services.length === 0 ? (
            <Empty className="border">
              <EmptyHeader>
                <EmptyMedia variant="icon">
                  <HugeiconsIcon icon={ServiceIcon} strokeWidth={2} />
                </EmptyMedia>
                <EmptyTitle>No managed services yet</EmptyTitle>
                <EmptyDescription>
                  Add the first production service to make it available in
                  incident workflows.
                </EmptyDescription>
              </EmptyHeader>
              {teams.length > 0 ? (
                <EmptyContent>
                  <Button onClick={openCreateDialog} type="button">
                    <HugeiconsIcon
                      data-icon="inline-start"
                      icon={Add01Icon}
                      strokeWidth={2}
                    />
                    Create service
                  </Button>
                </EmptyContent>
              ) : null}
            </Empty>
          ) : null}

          {loadState === "ready" && services.length > 0 ? (
            <div className="rounded-lg border">
              <Table containerLabel="Scrollable managed services table">
                <TableCaption className="sr-only">
                  Managed services in the administrative catalog
                </TableCaption>
                <TableHeader>
                  <TableRow>
                    <TableHead scope="col">Service</TableHead>
                    <TableHead scope="col">Criticality</TableHead>
                    <TableHead scope="col">Owning team</TableHead>
                    <TableHead className="text-right" scope="col">
                      Actions
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {services.map((service) => (
                    <TableRow key={service.id}>
                      <TableCell className="max-w-sm whitespace-normal">
                        <div className="font-medium">{service.name}</div>
                        <div className="mt-1 line-clamp-2 text-muted-foreground">
                          {service.description}
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={criticalityVariant(service.criticality)}
                        >
                          {criticalityLabels[service.criticality]}
                        </Badge>
                      </TableCell>
                      <TableCell>{service.owningTeam.name}</TableCell>
                      <TableCell>
                        <div className="flex justify-end gap-2">
                          <Button
                            aria-label={`Edit ${service.name}`}
                            onClick={(event) =>
                              openEditDialog(event, service)
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
                            aria-label={`Delete ${service.name}`}
                            onClick={(event) =>
                              openDeleteDialog(event, service)
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
            {services.length}{" "}
            {services.length === 1 ? "managed service" : "managed services"}
          </CardFooter>
        ) : null}
      </Card>

      {serviceForm?.mode === "create" ? (
        <ServiceFormDialog
          finalFocusRef={formFinalFocusRef}
          mode="create"
          onOpenChange={setIsServiceFormOpen}
          onSubmit={handleCreate}
          open={isServiceFormOpen}
          teams={teams}
        />
      ) : null}

      {serviceForm?.mode === "edit" ? (
        <ServiceFormDialog
          finalFocusRef={formFinalFocusRef}
          mode="edit"
          onOpenChange={setIsServiceFormOpen}
          onSubmit={(request) =>
            handleUpdate(serviceForm.service, request)
          }
          open={isServiceFormOpen}
          service={serviceForm.service}
          teams={teams}
        />
      ) : null}

      {serviceToDelete ? (
        <DeleteServiceDialog
          finalFocusRef={deleteFinalFocusRef}
          onDelete={handleDelete}
          onOpenChange={setIsDeleteDialogOpen}
          open={isDeleteDialogOpen}
          service={serviceToDelete}
        />
      ) : null}
    </>
  )
}

function ServiceListSkeleton() {
  return (
    <div
      aria-label="Loading managed services"
      aria-live="polite"
      className="flex flex-col gap-3"
      role="status"
    >
      <span className="sr-only">Loading managed services…</span>
      <Skeleton className="h-8 w-full" />
      <Skeleton className="h-16 w-full" />
      <Skeleton className="h-16 w-full" />
      <Skeleton className="h-16 w-full" />
    </div>
  )
}
