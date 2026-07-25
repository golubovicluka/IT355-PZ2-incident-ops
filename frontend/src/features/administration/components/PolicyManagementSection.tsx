import {
  Add01Icon,
  AlertCircleIcon,
  CheckmarkCircle02Icon,
  Delete02Icon,
  Loading03Icon,
  PencilEdit01Icon,
  PolicyIcon,
  RefreshIcon,
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
  createEscalationPolicy,
  deleteEscalationPolicy,
  listEscalationPolicies,
  updateEscalationPolicy,
} from "@/features/administration/api/policies-api"
import { listManagedServices } from "@/features/administration/api/services-api"
import { DeletePolicyDialog } from "@/features/administration/components/DeletePolicyDialog"
import { PolicyFormDialog } from "@/features/administration/components/PolicyFormDialog"
import type {
  EscalationPolicy,
  EscalationPolicyRequest,
  IncidentPriority,
} from "@/features/administration/model/policy.types"
import { MANAGED_SERVICES_CHANGED_EVENT } from "@/features/administration/model/administration.events"
import type { ManagedService } from "@/features/administration/model/service.types"
import { ApiError } from "@/shared/api/api-error"

type LoadState = "loading" | "ready" | "error"

type PolicyFormState =
  | {
      mode: "create"
    }
  | {
      mode: "edit"
      policy: EscalationPolicy
    }

const priorityLabels: Record<IncidentPriority, string> = {
  SEV1: "Critical",
  SEV2: "High",
  SEV3: "Medium",
  SEV4: "Low",
}

function sortPolicies(policies: EscalationPolicy[]) {
  return [...policies].sort(
    (first, second) =>
      first.managedService.name.localeCompare(
        second.managedService.name,
        undefined,
        { sensitivity: "base" },
      ) ||
      first.priority.localeCompare(second.priority) ||
      first.id - second.id,
  )
}

function sortServices(services: ManagedService[]) {
  return [...services].sort(
    (first, second) =>
      first.name.localeCompare(second.name, undefined, {
        sensitivity: "base",
      }) || first.id - second.id,
  )
}

function getRequestErrorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback
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

function formatMinutes(minutes: number) {
  return `${minutes.toLocaleString()} min`
}

export function PolicyManagementSection() {
  const [policies, setPolicies] = useState<EscalationPolicy[]>([])
  const [services, setServices] = useState<ManagedService[]>([])
  const [loadState, setLoadState] = useState<LoadState>("loading")
  const [loadError, setLoadError] = useState<string>()
  const [isRetrying, setIsRetrying] = useState(false)
  const [successMessage, setSuccessMessage] = useState<string>()
  const [policyForm, setPolicyForm] = useState<PolicyFormState>()
  const [isPolicyFormOpen, setIsPolicyFormOpen] = useState(false)
  const [policyToDelete, setPolicyToDelete] =
    useState<EscalationPolicy>()
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const sectionTitleRef = useRef<HTMLHeadingElement>(null)
  const createButtonRef = useRef<HTMLButtonElement>(null)
  const formFinalFocusRef = useRef<HTMLElement | null>(null)
  const deleteFinalFocusRef = useRef<HTMLElement | null>(null)

  useEffect(() => {
    const controller = new AbortController()

    async function loadData(signal?: AbortSignal) {
      try {
        const [policyResponse, serviceResponse] = await Promise.all([
          listEscalationPolicies(signal),
          listManagedServices(signal),
        ])
        setPolicies(sortPolicies(policyResponse))
        setServices(sortServices(serviceResponse))
        setLoadError(undefined)
        setLoadState("ready")
      } catch (error) {
        if (signal?.aborted) {
          return
        }

        setLoadError(
          getRequestErrorMessage(
            error,
            "Escalation policies or managed services are unavailable. Try loading them again.",
          ),
        )
        setLoadState("error")
      }
    }

    function handleManagedServicesChanged() {
      void loadData()
    }

    window.addEventListener(
      MANAGED_SERVICES_CHANGED_EVENT,
      handleManagedServicesChanged,
    )
    void loadData(controller.signal)

    return () => {
      controller.abort()
      window.removeEventListener(
        MANAGED_SERVICES_CHANGED_EVENT,
        handleManagedServicesChanged,
      )
    }
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
      const [policyResponse, serviceResponse] = await Promise.all([
        listEscalationPolicies(),
        listManagedServices(),
      ])
      setPolicies(sortPolicies(policyResponse))
      setServices(sortServices(serviceResponse))
      setLoadError(undefined)
      setLoadState("ready")
      requestAnimationFrame(() => sectionTitleRef.current?.focus())
    } catch (error) {
      setLoadError(
        getRequestErrorMessage(
          error,
          "Escalation policies or managed services are unavailable. Try loading them again.",
        ),
      )
    } finally {
      setIsRetrying(false)
    }
  }

  function openCreateDialog(event: MouseEvent<HTMLButtonElement>) {
    formFinalFocusRef.current = event.currentTarget
    setPolicyForm({ mode: "create" })
    setIsPolicyFormOpen(true)
  }

  function openEditDialog(
    event: MouseEvent<HTMLButtonElement>,
    policy: EscalationPolicy,
  ) {
    formFinalFocusRef.current = event.currentTarget
    setPolicyForm({ mode: "edit", policy })
    setIsPolicyFormOpen(true)
  }

  function openDeleteDialog(
    event: MouseEvent<HTMLButtonElement>,
    policy: EscalationPolicy,
  ) {
    deleteFinalFocusRef.current = event.currentTarget
    setPolicyToDelete(policy)
    setIsDeleteDialogOpen(true)
  }

  async function handleCreate(request: EscalationPolicyRequest) {
    setSuccessMessage(undefined)
    const created = await createEscalationPolicy(request)
    formFinalFocusRef.current = createButtonRef.current
    setPolicies((current) => sortPolicies([...current, created]))
    setSuccessMessage("Escalation policy created.")
  }

  async function handleUpdate(
    policy: EscalationPolicy,
    request: EscalationPolicyRequest,
  ) {
    setSuccessMessage(undefined)
    const updated = await updateEscalationPolicy(policy.id, request)
    setPolicies((current) =>
      sortPolicies(
        current.map((item) => (item.id === updated.id ? updated : item)),
      ),
    )
    setSuccessMessage("Escalation policy updated.")
  }

  async function handleDelete(policy: EscalationPolicy) {
    setSuccessMessage(undefined)
    await deleteEscalationPolicy(policy.id)
    deleteFinalFocusRef.current = sectionTitleRef.current
    setPolicies((current) =>
      current.filter((item) => item.id !== policy.id),
    )
    setSuccessMessage("Escalation policy deleted.")
  }

  const createDisabled = loadState !== "ready" || services.length === 0

  return (
    <>
      <Card className="scroll-mt-6" id="policies">
        <CardHeader>
          <CardTitle>
            <h2 ref={sectionTitleRef} tabIndex={-1}>
              Escalation policies
            </h2>
          </CardTitle>
          <CardDescription>
            Maintain acknowledgement and resolution deadlines by service and
            incident priority.
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
              Create policy
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

          {loadState === "loading" ? <PolicyListSkeleton /> : null}

          {loadState === "error" ? (
            <Alert variant="destructive">
              <HugeiconsIcon icon={AlertCircleIcon} strokeWidth={2} />
              <AlertTitle>Unable to load escalation policies</AlertTitle>
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

          {loadState === "ready" && services.length === 0 ? (
            <Alert>
              <HugeiconsIcon icon={AlertCircleIcon} strokeWidth={2} />
              <AlertTitle>Create a managed service first</AlertTitle>
              <AlertDescription>
                Escalation policies require a valid managed service. Add one
                in the Managed services section before creating a policy.
              </AlertDescription>
            </Alert>
          ) : null}

          {loadState === "ready" && policies.length === 0 ? (
            <Empty className="border">
              <EmptyHeader>
                <EmptyMedia variant="icon">
                  <HugeiconsIcon icon={PolicyIcon} strokeWidth={2} />
                </EmptyMedia>
                <EmptyTitle>No escalation policies yet</EmptyTitle>
                <EmptyDescription>
                  Add the first service and priority rule to define its SLA
                  deadlines.
                </EmptyDescription>
              </EmptyHeader>
              {services.length > 0 ? (
                <EmptyContent>
                  <Button onClick={openCreateDialog} type="button">
                    <HugeiconsIcon
                      data-icon="inline-start"
                      icon={Add01Icon}
                      strokeWidth={2}
                    />
                    Create policy
                  </Button>
                </EmptyContent>
              ) : null}
            </Empty>
          ) : null}

          {loadState === "ready" && policies.length > 0 ? (
            <div className="overflow-x-auto rounded-lg border">
              <Table containerLabel="Scrollable escalation policies table">
                <TableCaption className="sr-only">
                  Escalation policies in the administrative catalog
                </TableCaption>
                <TableHeader>
                  <TableRow>
                    <TableHead scope="col">Service</TableHead>
                    <TableHead scope="col">Priority</TableHead>
                    <TableHead scope="col">Acknowledge by</TableHead>
                    <TableHead scope="col">Resolve by</TableHead>
                    <TableHead className="text-right" scope="col">
                      Actions
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {policies.map((policy) => (
                    <TableRow key={policy.id}>
                      <TableCell className="font-medium">
                        {policy.managedService.name}
                      </TableCell>
                      <TableCell>
                        <Badge variant={priorityVariant(policy.priority)}>
                          {policy.priority} ·{" "}
                          {priorityLabels[policy.priority]}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {formatMinutes(policy.acknowledgementMinutes)}
                      </TableCell>
                      <TableCell>
                        {formatMinutes(policy.resolutionMinutes)}
                      </TableCell>
                      <TableCell>
                        <div className="flex justify-end gap-2">
                          <Button
                            aria-label={`Edit ${policy.managedService.name} ${policy.priority} policy`}
                            onClick={(event) =>
                              openEditDialog(event, policy)
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
                            aria-label={`Delete ${policy.managedService.name} ${policy.priority} policy`}
                            onClick={(event) =>
                              openDeleteDialog(event, policy)
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
            {policies.length}{" "}
            {policies.length === 1
              ? "escalation policy"
              : "escalation policies"}
          </CardFooter>
        ) : null}
      </Card>

      {policyForm?.mode === "create" ? (
        <PolicyFormDialog
          finalFocusRef={formFinalFocusRef}
          mode="create"
          onOpenChange={setIsPolicyFormOpen}
          onSubmit={handleCreate}
          open={isPolicyFormOpen}
          services={services}
        />
      ) : null}

      {policyForm?.mode === "edit" ? (
        <PolicyFormDialog
          finalFocusRef={formFinalFocusRef}
          mode="edit"
          onOpenChange={setIsPolicyFormOpen}
          onSubmit={(request) =>
            handleUpdate(policyForm.policy, request)
          }
          open={isPolicyFormOpen}
          policy={policyForm.policy}
          services={services}
        />
      ) : null}

      {policyToDelete ? (
        <DeletePolicyDialog
          finalFocusRef={deleteFinalFocusRef}
          onDelete={handleDelete}
          onOpenChange={setIsDeleteDialogOpen}
          open={isDeleteDialogOpen}
          policy={policyToDelete}
        />
      ) : null}
    </>
  )
}

function PolicyListSkeleton() {
  return (
    <div
      aria-label="Loading escalation policies"
      aria-live="polite"
      className="flex flex-col gap-3"
      role="status"
    >
      <span className="sr-only">Loading escalation policies…</span>
      <Skeleton className="h-8 w-full" />
      <Skeleton className="h-12 w-full" />
      <Skeleton className="h-12 w-full" />
      <Skeleton className="h-12 w-full" />
    </div>
  )
}
