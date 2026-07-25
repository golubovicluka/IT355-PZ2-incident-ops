import {
  AlertTriangleIcon,
  LoaderCircleIcon,
  RefreshCwIcon,
} from "lucide-react"
import {
  useEffect,
  useRef,
  useState,
  type ChangeEvent,
  type FormEvent,
  type RefObject,
} from "react"

import {
  Alert,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import {
  incidentPriorities,
  type IncidentDetail,
  type IncidentPriority,
  type IncidentRequest,
} from "@/features/incidents/model/incident.types"
import {
  INCIDENT_DESCRIPTION_MAX_LENGTH,
  INCIDENT_TITLE_MAX_LENGTH,
  normalizeIncidentForm,
  type IncidentFormErrors,
  type IncidentFormValues,
  validateIncidentForm,
} from "@/features/incidents/model/incident.validation"
import { ApiError } from "@/shared/api/api-error"
import {
  listAssignableUserCatalog,
  listServiceCatalog,
} from "@/shared/catalogs/catalog-api"
import type {
  AssignableUserCatalogItem,
  ManagedServiceCatalogItem,
} from "@/shared/catalogs/catalog.types"

type CatalogState = "loading" | "ready" | "error"

interface SharedIncidentFormDialogProps {
  finalFocusRef: RefObject<HTMLElement | null>
  onOpenChange: (open: boolean) => void
  onSubmit: (request: IncidentRequest) => Promise<IncidentDetail>
  onSuccess: (incident: IncidentDetail) => void
  open: boolean
}

type IncidentFormDialogProps = SharedIncidentFormDialogProps &
  (
    | {
        incident?: never
        mode: "create"
      }
    | {
        incident: IncidentDetail
        mode: "edit"
      }
  )

function formValuesFor(
  mode: IncidentFormDialogProps["mode"],
  incident?: IncidentDetail,
): IncidentFormValues {
  if (mode === "edit" && incident) {
    return {
      title: incident.title,
      description: incident.description,
      priority: incident.priority,
      managedServiceId: incident.managedService.id,
      assigneeId: incident.assignee?.id ?? null,
    }
  }

  return {
    title: "",
    description: "",
    priority: null,
    managedServiceId: null,
    assigneeId: null,
  }
}

export function IncidentFormDialog({
  finalFocusRef,
  incident,
  mode,
  onOpenChange,
  onSubmit,
  onSuccess,
  open,
}: IncidentFormDialogProps) {
  const [values, setValues] = useState<IncidentFormValues>(() =>
    formValuesFor(mode, incident),
  )
  const [fieldErrors, setFieldErrors] = useState<IncidentFormErrors>({})
  const [submitError, setSubmitError] = useState<string>()
  const [isPending, setIsPending] = useState(false)
  const [catalogState, setCatalogState] =
    useState<CatalogState>("loading")
  const [catalogError, setCatalogError] = useState<string>()
  const [catalogVersion, setCatalogVersion] = useState(0)
  const [services, setServices] = useState<ManagedServiceCatalogItem[]>([])
  const [users, setUsers] = useState<AssignableUserCatalogItem[]>([])
  const titleRef = useRef<HTMLInputElement>(null)
  const errorFeedbackRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) {
      return
    }

    setValues(formValuesFor(mode, incident))
    setFieldErrors({})
    setSubmitError(undefined)
    setIsPending(false)
  }, [incident, mode, open])

  useEffect(() => {
    if (!open) {
      return
    }

    const controller = new AbortController()

    async function loadCatalogs() {
      setCatalogState("loading")
      setCatalogError(undefined)

      try {
        const [serviceResponse, userResponse] = await Promise.all([
          listServiceCatalog(controller.signal),
          listAssignableUserCatalog(controller.signal),
        ])
        setServices(
          [...serviceResponse].sort((first, second) =>
            first.name.localeCompare(second.name),
          ),
        )
        setUsers(
          [...userResponse].sort(
            (first, second) =>
              first.displayName.localeCompare(second.displayName) ||
              first.id - second.id,
          ),
        )
        setCatalogState("ready")
      } catch (error) {
        if (!controller.signal.aborted) {
          setCatalogError(
            error instanceof ApiError
              ? error.message
              : "Service and assignee choices are unavailable. Try again.",
          )
          setCatalogState("error")
        }
      }
    }

    void loadCatalogs()
    return () => controller.abort()
  }, [catalogVersion, open])

  useEffect(() => {
    if (submitError) {
      requestAnimationFrame(() => errorFeedbackRef.current?.focus())
    }
  }, [submitError])

  function clearError(field: keyof IncidentFormErrors) {
    setFieldErrors((current) => ({ ...current, [field]: undefined }))
    setSubmitError(undefined)
  }

  function handleTitleChange(event: ChangeEvent<HTMLInputElement>) {
    setValues((current) => ({ ...current, title: event.target.value }))
    clearError("title")
  }

  function handleDescriptionChange(
    event: ChangeEvent<HTMLTextAreaElement>,
  ) {
    setValues((current) => ({
      ...current,
      description: event.target.value,
    }))
    clearError("description")
  }

  function handleOpenChange(nextOpen: boolean) {
    if (!isPending) {
      onOpenChange(nextOpen)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (
      isPending ||
      catalogState !== "ready" ||
      services.length === 0
    ) {
      return
    }

    const normalized = normalizeIncidentForm(values)
    const errors = validateIncidentForm(normalized)
    setFieldErrors(errors)

    if (
      Object.keys(errors).length > 0 ||
      !normalized.priority ||
      !normalized.managedServiceId
    ) {
      setSubmitError("Review the highlighted fields and try again.")
      return
    }

    setIsPending(true)
    setSubmitError(undefined)

    try {
      const saved = await onSubmit({
        title: normalized.title,
        description: normalized.description,
        priority: normalized.priority,
        managedServiceId: normalized.managedServiceId,
        assigneeId: normalized.assigneeId,
      })
      setIsPending(false)
      onSuccess(saved)
      onOpenChange(false)
    } catch (error) {
      if (error instanceof ApiError) {
        setFieldErrors({
          title: error.fieldErrors.title,
          description: error.fieldErrors.description,
          priority: error.fieldErrors.priority,
          managedServiceId: error.fieldErrors.managedServiceId,
          assigneeId: error.fieldErrors.assigneeId,
        })
        setSubmitError(error.message)
      } else {
        setSubmitError("The incident could not be saved. Try again.")
      }
      setIsPending(false)
    }
  }

  const isEditing = mode === "edit"
  const prefix = isEditing ? `edit-incident-${incident.id}` : "new-incident"

  return (
    <Dialog onOpenChange={handleOpenChange} open={open}>
      <DialogContent
        className="max-h-[calc(100svh-2rem)] overflow-y-auto sm:max-w-2xl"
        finalFocus={finalFocusRef}
        initialFocus={titleRef}
        showCloseButton={!isPending}
      >
        <form className="flex flex-col gap-4" noValidate onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>
              {isEditing ? "Edit incident" : "Report an incident"}
            </DialogTitle>
            <DialogDescription>
              {isEditing
                ? "Correct the incident context and current ownership."
                : "Record the affected service, priority, and assignee."}
            </DialogDescription>
          </DialogHeader>

          {catalogState === "loading" ? (
            <div
              aria-label="Loading incident form"
              className="space-y-3"
              role="status"
            >
              <Skeleton className="h-8 w-full" />
              <Skeleton className="h-24 w-full" />
              <Skeleton className="h-8 w-full" />
            </div>
          ) : null}

          {catalogState === "error" ? (
            <Alert variant="destructive">
              <AlertTriangleIcon />
              <AlertTitle>Unable to load incident choices</AlertTitle>
              <AlertDescription>
                <p>{catalogError}</p>
                <Button
                  className="mt-3"
                  onClick={() => setCatalogVersion((current) => current + 1)}
                  size="sm"
                  type="button"
                  variant="outline"
                >
                  <RefreshCwIcon />
                  Retry
                </Button>
              </AlertDescription>
            </Alert>
          ) : null}

          {catalogState === "ready" ? (
            <>
              {services.length === 0 ? (
                <Alert variant="destructive">
                  <AlertTriangleIcon />
                  <AlertTitle>No managed services available</AlertTitle>
                  <AlertDescription>
                    An administrator must add a managed service before this
                    incident can be saved.
                  </AlertDescription>
                </Alert>
              ) : null}

              <FieldGroup>
              <Field
                data-disabled={isPending || undefined}
                data-invalid={Boolean(fieldErrors.title)}
              >
                <FieldLabel htmlFor={`${prefix}-title`}>Title</FieldLabel>
                <Input
                  aria-invalid={Boolean(fieldErrors.title) || undefined}
                  autoComplete="off"
                  disabled={isPending}
                  id={`${prefix}-title`}
                  maxLength={INCIDENT_TITLE_MAX_LENGTH}
                  onChange={handleTitleChange}
                  placeholder="e.g. Checkout failures"
                  ref={titleRef}
                  value={values.title}
                />
                <FieldDescription>
                  A concise operational summary, up to{" "}
                  {INCIDENT_TITLE_MAX_LENGTH} characters.
                </FieldDescription>
                {fieldErrors.title ? (
                  <FieldError>{fieldErrors.title}</FieldError>
                ) : null}
              </Field>

              <Field
                data-disabled={isPending || undefined}
                data-invalid={Boolean(fieldErrors.description)}
              >
                <FieldLabel htmlFor={`${prefix}-description`}>
                  Description
                </FieldLabel>
                <Textarea
                  aria-invalid={
                    Boolean(fieldErrors.description) || undefined
                  }
                  disabled={isPending}
                  id={`${prefix}-description`}
                  maxLength={INCIDENT_DESCRIPTION_MAX_LENGTH}
                  onChange={handleDescriptionChange}
                  placeholder="Describe the impact and observable symptoms."
                  rows={5}
                  value={values.description}
                />
                <FieldDescription>
                  Up to {INCIDENT_DESCRIPTION_MAX_LENGTH} characters.
                </FieldDescription>
                {fieldErrors.description ? (
                  <FieldError>{fieldErrors.description}</FieldError>
                ) : null}
              </Field>

              <div className="grid gap-4 sm:grid-cols-2">
                <Field
                  data-disabled={isPending || undefined}
                  data-invalid={Boolean(fieldErrors.priority)}
                >
                  <FieldLabel htmlFor={`${prefix}-priority`}>
                    Priority
                  </FieldLabel>
                  <Select<IncidentPriority>
                    disabled={isPending}
                    id={`${prefix}-priority`}
                    onValueChange={(value) => {
                      setValues((current) => ({
                        ...current,
                        priority: value,
                      }))
                      clearError("priority")
                    }}
                    value={values.priority}
                  >
                    <SelectTrigger
                      aria-invalid={
                        Boolean(fieldErrors.priority) || undefined
                      }
                      aria-label="Priority"
                      className="w-full"
                    >
                      <SelectValue placeholder="Select priority">
                        {(value: IncidentPriority | null) =>
                          value ?? "Select priority"
                        }
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {incidentPriorities.map((priority) => (
                        <SelectItem key={priority} value={priority}>
                          {priority}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {fieldErrors.priority ? (
                    <FieldError>{fieldErrors.priority}</FieldError>
                  ) : null}
                </Field>

                <Field
                  data-disabled={isPending || undefined}
                  data-invalid={Boolean(fieldErrors.managedServiceId)}
                >
                  <FieldLabel htmlFor={`${prefix}-service`}>
                    Managed service
                  </FieldLabel>
                  <Select<number>
                    disabled={isPending || services.length === 0}
                    id={`${prefix}-service`}
                    onValueChange={(value) => {
                      setValues((current) => ({
                        ...current,
                        managedServiceId: value,
                      }))
                      clearError("managedServiceId")
                    }}
                    value={values.managedServiceId}
                  >
                    <SelectTrigger
                      aria-invalid={
                        Boolean(fieldErrors.managedServiceId) || undefined
                      }
                      aria-label="Managed service"
                      className="w-full"
                    >
                      <SelectValue placeholder="Select service">
                        {(value: number | null) =>
                          value
                            ? services.find((service) => service.id === value)
                                ?.name ?? `Service ${value}`
                            : "Select service"
                        }
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {services.map((service) => (
                        <SelectItem key={service.id} value={service.id}>
                          {service.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {fieldErrors.managedServiceId ? (
                    <FieldError>{fieldErrors.managedServiceId}</FieldError>
                  ) : null}
                </Field>
              </div>

              <Field
                data-disabled={isPending || undefined}
                data-invalid={Boolean(fieldErrors.assigneeId)}
              >
                <FieldLabel htmlFor={`${prefix}-assignee`}>
                  Assignee
                </FieldLabel>
                <Select<number>
                  disabled={isPending}
                  id={`${prefix}-assignee`}
                  onValueChange={(value) => {
                    setValues((current) => ({
                      ...current,
                      assigneeId: value || null,
                    }))
                    clearError("assigneeId")
                  }}
                  value={values.assigneeId ?? 0}
                >
                  <SelectTrigger
                    aria-invalid={
                      Boolean(fieldErrors.assigneeId) || undefined
                    }
                    aria-label="Assignee"
                    className="w-full"
                  >
                    <SelectValue>
                      {(value: number | null) => {
                        const selected = users.find(
                          (user) => user.id === value,
                        )
                        const currentAssignee =
                          isEditing && incident.assignee?.id === value
                            ? incident.assignee
                            : undefined
                        return selected
                          ? `${selected.displayName} (${selected.username})`
                          : currentAssignee
                            ? `${currentAssignee.displayName} (${currentAssignee.username})`
                          : "Unassigned"
                      }}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={0}>Unassigned</SelectItem>
                    {users.map((user) => (
                      <SelectItem key={user.id} value={user.id}>
                        {user.displayName} ({user.username})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FieldDescription>
                  {users.length === 0
                    ? "No assignable users are available. This incident can remain unassigned."
                    : "Assignment is optional and can be changed later."}
                </FieldDescription>
                {fieldErrors.assigneeId ? (
                  <FieldError>{fieldErrors.assigneeId}</FieldError>
                ) : null}
              </Field>
              </FieldGroup>
            </>
          ) : null}

          {submitError ? (
            <Alert
              ref={errorFeedbackRef}
              tabIndex={-1}
              variant="destructive"
            >
              <AlertTriangleIcon />
              <AlertTitle>Unable to save incident</AlertTitle>
              <AlertDescription>{submitError}</AlertDescription>
            </Alert>
          ) : null}

          <DialogFooter>
            <Button
              disabled={isPending}
              onClick={() => onOpenChange(false)}
              type="button"
              variant="outline"
            >
              Cancel
            </Button>
            <Button
              disabled={
                isPending ||
                catalogState !== "ready" ||
                services.length === 0
              }
              type="submit"
            >
              {isPending ? (
                <LoaderCircleIcon className="animate-spin" />
              ) : null}
              {isPending
                ? isEditing
                  ? "Saving changes..."
                  : "Reporting..."
                : isEditing
                  ? "Save changes"
                  : "Report incident"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
