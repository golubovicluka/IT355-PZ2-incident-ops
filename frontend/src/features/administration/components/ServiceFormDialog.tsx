import {
  AlertCircleIcon,
  Add01Icon,
  Loading03Icon,
  PencilEdit01Icon,
} from "@hugeicons/core-free-icons"
import { HugeiconsIcon } from "@hugeicons/react"
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
import { Textarea } from "@/components/ui/textarea"
import type {
  ManagedService,
  ManagedServiceRequest,
} from "@/features/administration/model/service.types"
import {
  normalizeServiceForm,
  SERVICE_DESCRIPTION_MAX_LENGTH,
  SERVICE_NAME_MAX_LENGTH,
  type ServiceFormErrors,
  type ServiceFormValues,
  validateServiceForm,
} from "@/features/administration/model/service.validation"
import { ApiError } from "@/shared/api/api-error"
import type {
  CatalogTeam,
  Criticality,
} from "@/shared/catalogs/catalog.types"

const criticalities: Criticality[] = [
  "LOW",
  "MEDIUM",
  "HIGH",
  "CRITICAL",
]

const criticalityLabels: Record<Criticality, string> = {
  LOW: "Low",
  MEDIUM: "Medium",
  HIGH: "High",
  CRITICAL: "Critical",
}

interface SharedServiceFormDialogProps {
  finalFocusRef: RefObject<HTMLElement | null>
  onOpenChange: (open: boolean) => void
  onSubmit: (request: ManagedServiceRequest) => Promise<void>
  open: boolean
  teams: CatalogTeam[]
}

type ServiceFormDialogProps = SharedServiceFormDialogProps &
  (
    | {
        mode: "create"
        service?: never
      }
    | {
        mode: "edit"
        service: ManagedService
      }
  )

export function ServiceFormDialog({
  finalFocusRef,
  mode,
  onOpenChange,
  onSubmit,
  open,
  service,
  teams,
}: ServiceFormDialogProps) {
  const initialName = mode === "edit" ? service.name : ""
  const initialDescription = mode === "edit" ? service.description : ""
  const initialCriticality = mode === "edit" ? service.criticality : null
  const initialTeamId = mode === "edit" ? service.owningTeam.id : null
  const prefix = mode === "edit" ? `service-${service.id}` : "new-service"
  const [values, setValues] = useState<ServiceFormValues>({
    name: initialName,
    description: initialDescription,
    criticality: initialCriticality,
    owningTeamId: initialTeamId,
  })
  const [fieldErrors, setFieldErrors] = useState<ServiceFormErrors>({})
  const [submitError, setSubmitError] = useState<string>()
  const [isPending, setIsPending] = useState(false)
  const nameRef = useRef<HTMLInputElement>(null)
  const errorFeedbackRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) {
      return
    }

    setValues({
      name: initialName,
      description: initialDescription,
      criticality: initialCriticality,
      owningTeamId: initialTeamId,
    })
    setFieldErrors({})
    setSubmitError(undefined)
    setIsPending(false)
  }, [
    initialCriticality,
    initialDescription,
    initialName,
    initialTeamId,
    open,
  ])

  useEffect(() => {
    if (submitError) {
      requestAnimationFrame(() => errorFeedbackRef.current?.focus())
    }
  }, [submitError])

  function clearError(field: keyof ServiceFormErrors) {
    setFieldErrors((current) => ({ ...current, [field]: undefined }))
    setSubmitError(undefined)
  }

  function handleNameChange(event: ChangeEvent<HTMLInputElement>) {
    setValues((current) => ({ ...current, name: event.target.value }))
    clearError("name")
  }

  function handleDescriptionChange(event: ChangeEvent<HTMLTextAreaElement>) {
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

    if (isPending) {
      return
    }

    const normalized = normalizeServiceForm(values)
    const errors = validateServiceForm(normalized)
    setFieldErrors(errors)

    if (
      Object.keys(errors).length > 0 ||
      !normalized.criticality ||
      !normalized.owningTeamId
    ) {
      requestAnimationFrame(() => nameRef.current?.focus())
      return
    }

    setIsPending(true)
    setSubmitError(undefined)

    try {
      await onSubmit({
        name: normalized.name,
        description: normalized.description,
        criticality: normalized.criticality,
        owningTeamId: normalized.owningTeamId,
      })
      setIsPending(false)
      onOpenChange(false)
    } catch (error) {
      if (error instanceof ApiError) {
        setFieldErrors({
          name: error.fieldErrors.name,
          description: error.fieldErrors.description,
          criticality: error.fieldErrors.criticality,
          owningTeamId: error.fieldErrors.owningTeamId,
        })
        setSubmitError(error.message)
      } else {
        setSubmitError("The managed service could not be saved. Try again.")
      }
      setIsPending(false)
    }
  }

  const isEditing = mode === "edit"

  return (
    <Dialog onOpenChange={handleOpenChange} open={open}>
      <DialogContent
        finalFocus={finalFocusRef}
        initialFocus={nameRef}
        showCloseButton={!isPending}
      >
        <form
          className="flex flex-col gap-4"
          noValidate
          onSubmit={handleSubmit}
        >
          <DialogHeader>
            <DialogTitle>
              {isEditing ? "Edit managed service" : "Create managed service"}
            </DialogTitle>
            <DialogDescription>
              Set the incident-facing details and the team accountable for
              this service.
            </DialogDescription>
          </DialogHeader>

          <FieldGroup>
            <Field
              data-disabled={isPending || undefined}
              data-invalid={Boolean(fieldErrors.name)}
            >
              <FieldLabel htmlFor={`${prefix}-name`}>Service name</FieldLabel>
              <Input
                aria-invalid={Boolean(fieldErrors.name) || undefined}
                autoComplete="off"
                disabled={isPending}
                id={`${prefix}-name`}
                onChange={handleNameChange}
                placeholder="e.g. Payments API"
                ref={nameRef}
                value={values.name}
              />
              <FieldDescription>
                A unique operational name, up to {SERVICE_NAME_MAX_LENGTH}{" "}
                characters.
              </FieldDescription>
              {fieldErrors.name ? (
                <FieldError>{fieldErrors.name}</FieldError>
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
                aria-invalid={Boolean(fieldErrors.description) || undefined}
                disabled={isPending}
                id={`${prefix}-description`}
                onChange={handleDescriptionChange}
                placeholder="Describe what the service does and who depends on it."
                value={values.description}
              />
              <FieldDescription>
                Up to {SERVICE_DESCRIPTION_MAX_LENGTH} characters.
              </FieldDescription>
              {fieldErrors.description ? (
                <FieldError>{fieldErrors.description}</FieldError>
              ) : null}
            </Field>

            <div className="grid gap-4 sm:grid-cols-2">
              <Field
                data-disabled={isPending || undefined}
                data-invalid={Boolean(fieldErrors.criticality)}
              >
                <FieldLabel htmlFor={`${prefix}-criticality`}>
                  Criticality
                </FieldLabel>
                <Select<Criticality>
                  disabled={isPending}
                  id={`${prefix}-criticality`}
                  onValueChange={(value) => {
                    setValues((current) => ({
                      ...current,
                      criticality: value,
                    }))
                    clearError("criticality")
                  }}
                  value={values.criticality}
                >
                  <SelectTrigger
                    aria-invalid={
                      Boolean(fieldErrors.criticality) || undefined
                    }
                    className="w-full"
                  >
                    <SelectValue placeholder="Select criticality">
                      {(value: Criticality | null) =>
                        value ? criticalityLabels[value] : "Select criticality"
                      }
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {criticalities.map((criticality) => (
                      <SelectItem key={criticality} value={criticality}>
                        {criticalityLabels[criticality]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {fieldErrors.criticality ? (
                  <FieldError>{fieldErrors.criticality}</FieldError>
                ) : null}
              </Field>

              <Field
                data-disabled={isPending || undefined}
                data-invalid={Boolean(fieldErrors.owningTeamId)}
              >
                <FieldLabel htmlFor={`${prefix}-team`}>
                  Owning team
                </FieldLabel>
                <Select<number>
                  disabled={isPending}
                  id={`${prefix}-team`}
                  onValueChange={(value) => {
                    setValues((current) => ({
                      ...current,
                      owningTeamId: value,
                    }))
                    clearError("owningTeamId")
                  }}
                  value={values.owningTeamId}
                >
                  <SelectTrigger
                    aria-invalid={
                      Boolean(fieldErrors.owningTeamId) || undefined
                    }
                    className="w-full"
                  >
                    <SelectValue placeholder="Select team">
                      {(value: number | null) =>
                        teams.find((team) => team.id === value)?.name ??
                        "Select team"
                      }
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {teams.map((team) => (
                      <SelectItem key={team.id} value={team.id}>
                        {team.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {fieldErrors.owningTeamId ? (
                  <FieldError>{fieldErrors.owningTeamId}</FieldError>
                ) : null}
              </Field>
            </div>
          </FieldGroup>

          {submitError ? (
            <Alert
              ref={errorFeedbackRef}
              tabIndex={-1}
              variant="destructive"
            >
              <HugeiconsIcon icon={AlertCircleIcon} strokeWidth={2} />
              <AlertTitle>
                {isEditing
                  ? "Managed service could not be updated"
                  : "Managed service could not be created"}
              </AlertTitle>
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
            <Button disabled={isPending} type="submit">
              <HugeiconsIcon
                className={isPending ? "animate-spin" : undefined}
                data-icon="inline-start"
                icon={
                  isPending
                    ? Loading03Icon
                    : isEditing
                      ? PencilEdit01Icon
                      : Add01Icon
                }
                strokeWidth={2}
              />
              {isPending
                ? isEditing
                  ? "Saving…"
                  : "Creating…"
                : isEditing
                  ? "Save changes"
                  : "Create service"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
