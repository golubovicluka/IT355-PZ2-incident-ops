import {
  Add01Icon,
  AlertCircleIcon,
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
import type {
  EscalationPolicy,
  EscalationPolicyRequest,
  IncidentPriority,
} from "@/features/administration/model/policy.types"
import {
  toPositiveWholeMinutes,
  type PolicyFormErrors,
  type PolicyFormValues,
  validatePolicyForm,
} from "@/features/administration/model/policy.validation"
import type { ManagedService } from "@/features/administration/model/service.types"
import { ApiError } from "@/shared/api/api-error"

const priorities: IncidentPriority[] = ["SEV1", "SEV2", "SEV3", "SEV4"]

const priorityLabels: Record<IncidentPriority, string> = {
  SEV1: "SEV1 — Critical",
  SEV2: "SEV2 — High",
  SEV3: "SEV3 — Medium",
  SEV4: "SEV4 — Low",
}

interface SharedPolicyFormDialogProps {
  finalFocusRef: RefObject<HTMLElement | null>
  onOpenChange: (open: boolean) => void
  onSubmit: (request: EscalationPolicyRequest) => Promise<void>
  open: boolean
  services: ManagedService[]
}

type PolicyFormDialogProps = SharedPolicyFormDialogProps &
  (
    | {
        mode: "create"
        policy?: never
      }
    | {
        mode: "edit"
        policy: EscalationPolicy
      }
  )

export function PolicyFormDialog({
  finalFocusRef,
  mode,
  onOpenChange,
  onSubmit,
  open,
  policy,
  services,
}: PolicyFormDialogProps) {
  const initialServiceId =
    mode === "edit" ? policy.managedService.id : null
  const initialPriority = mode === "edit" ? policy.priority : null
  const initialAcknowledgementMinutes =
    mode === "edit" ? String(policy.acknowledgementMinutes) : ""
  const initialResolutionMinutes =
    mode === "edit" ? String(policy.resolutionMinutes) : ""
  const prefix = mode === "edit" ? `policy-${policy.id}` : "new-policy"
  const [values, setValues] = useState<PolicyFormValues>({
    managedServiceId: initialServiceId,
    priority: initialPriority,
    acknowledgementMinutes: initialAcknowledgementMinutes,
    resolutionMinutes: initialResolutionMinutes,
  })
  const [fieldErrors, setFieldErrors] = useState<PolicyFormErrors>({})
  const [submitError, setSubmitError] = useState<string>()
  const [isPending, setIsPending] = useState(false)
  const acknowledgementRef = useRef<HTMLInputElement>(null)
  const errorFeedbackRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) {
      return
    }

    setValues({
      managedServiceId: initialServiceId,
      priority: initialPriority,
      acknowledgementMinutes: initialAcknowledgementMinutes,
      resolutionMinutes: initialResolutionMinutes,
    })
    setFieldErrors({})
    setSubmitError(undefined)
    setIsPending(false)
  }, [
    initialAcknowledgementMinutes,
    initialPriority,
    initialResolutionMinutes,
    initialServiceId,
    open,
  ])

  useEffect(() => {
    if (submitError) {
      requestAnimationFrame(() => errorFeedbackRef.current?.focus())
    }
  }, [submitError])

  function clearError(field: keyof PolicyFormErrors) {
    setFieldErrors((current) => ({ ...current, [field]: undefined }))
    setSubmitError(undefined)
  }

  function handleDeadlineChange(
    field: "acknowledgementMinutes" | "resolutionMinutes",
    event: ChangeEvent<HTMLInputElement>,
  ) {
    setValues((current) => ({
      ...current,
      [field]: event.target.value,
    }))
    clearError(field)
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

    const errors = validatePolicyForm(values)
    const acknowledgementMinutes = toPositiveWholeMinutes(
      values.acknowledgementMinutes,
    )
    const resolutionMinutes = toPositiveWholeMinutes(
      values.resolutionMinutes,
    )
    setFieldErrors(errors)

    if (
      Object.keys(errors).length > 0 ||
      !values.managedServiceId ||
      !values.priority ||
      !acknowledgementMinutes ||
      !resolutionMinutes
    ) {
      requestAnimationFrame(() => acknowledgementRef.current?.focus())
      return
    }

    setIsPending(true)
    setSubmitError(undefined)

    try {
      await onSubmit({
        managedServiceId: values.managedServiceId,
        priority: values.priority,
        acknowledgementMinutes,
        resolutionMinutes,
      })
      setIsPending(false)
      onOpenChange(false)
    } catch (error) {
      if (error instanceof ApiError) {
        setFieldErrors({
          managedServiceId: error.fieldErrors.managedServiceId,
          priority: error.fieldErrors.priority,
          acknowledgementMinutes:
            error.fieldErrors.acknowledgementMinutes,
          resolutionMinutes: error.fieldErrors.resolutionMinutes,
        })
        setSubmitError(error.message)
      } else {
        setSubmitError(
          "The escalation policy could not be saved. Try again.",
        )
      }
      setIsPending(false)
    }
  }

  const isEditing = mode === "edit"

  return (
    <Dialog onOpenChange={handleOpenChange} open={open}>
      <DialogContent
        finalFocus={finalFocusRef}
        initialFocus={acknowledgementRef}
        showCloseButton={!isPending}
      >
        <form
          className="flex flex-col gap-4"
          noValidate
          onSubmit={handleSubmit}
        >
          <DialogHeader>
            <DialogTitle>
              {isEditing
                ? "Edit escalation policy"
                : "Create escalation policy"}
            </DialogTitle>
            <DialogDescription>
              Define acknowledgement and resolution targets for one service
              and incident priority.
            </DialogDescription>
          </DialogHeader>

          <FieldGroup>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field
                data-disabled={isPending || undefined}
                data-invalid={Boolean(fieldErrors.managedServiceId)}
              >
                <FieldLabel htmlFor={`${prefix}-service`}>
                  Managed service
                </FieldLabel>
                <Select<number>
                  disabled={isPending}
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
                    className="w-full"
                  >
                    <SelectValue placeholder="Select service">
                      {(value: number | null) =>
                        services.find((service) => service.id === value)
                          ?.name ?? "Select service"
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

              <Field
                data-disabled={isPending || undefined}
                data-invalid={Boolean(fieldErrors.priority)}
              >
                <FieldLabel htmlFor={`${prefix}-priority`}>
                  Incident priority
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
                    className="w-full"
                  >
                    <SelectValue placeholder="Select priority">
                      {(value: IncidentPriority | null) =>
                        value
                          ? priorityLabels[value]
                          : "Select priority"
                      }
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {priorities.map((priority) => (
                      <SelectItem key={priority} value={priority}>
                        {priorityLabels[priority]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {fieldErrors.priority ? (
                  <FieldError>{fieldErrors.priority}</FieldError>
                ) : null}
              </Field>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <Field
                data-disabled={isPending || undefined}
                data-invalid={Boolean(
                  fieldErrors.acknowledgementMinutes,
                )}
              >
                <FieldLabel htmlFor={`${prefix}-acknowledgement`}>
                  Acknowledgement deadline
                </FieldLabel>
                <Input
                  aria-invalid={
                    Boolean(fieldErrors.acknowledgementMinutes) ||
                    undefined
                  }
                  disabled={isPending}
                  id={`${prefix}-acknowledgement`}
                  inputMode="numeric"
                  min="1"
                  onChange={(event) =>
                    handleDeadlineChange(
                      "acknowledgementMinutes",
                      event,
                    )
                  }
                  placeholder="e.g. 15"
                  ref={acknowledgementRef}
                  step="1"
                  type="number"
                  value={values.acknowledgementMinutes}
                />
                <FieldDescription>Whole minutes from creation.</FieldDescription>
                {fieldErrors.acknowledgementMinutes ? (
                  <FieldError>
                    {fieldErrors.acknowledgementMinutes}
                  </FieldError>
                ) : null}
              </Field>

              <Field
                data-disabled={isPending || undefined}
                data-invalid={Boolean(fieldErrors.resolutionMinutes)}
              >
                <FieldLabel htmlFor={`${prefix}-resolution`}>
                  Resolution deadline
                </FieldLabel>
                <Input
                  aria-invalid={
                    Boolean(fieldErrors.resolutionMinutes) || undefined
                  }
                  disabled={isPending}
                  id={`${prefix}-resolution`}
                  inputMode="numeric"
                  min="1"
                  onChange={(event) =>
                    handleDeadlineChange("resolutionMinutes", event)
                  }
                  placeholder="e.g. 120"
                  step="1"
                  type="number"
                  value={values.resolutionMinutes}
                />
                <FieldDescription>
                  Must be at least the acknowledgement deadline.
                </FieldDescription>
                {fieldErrors.resolutionMinutes ? (
                  <FieldError>{fieldErrors.resolutionMinutes}</FieldError>
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
                  ? "Escalation policy could not be updated"
                  : "Escalation policy could not be created"}
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
                  : "Create policy"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
