import { useId, useRef, useState, type FormEvent } from "react"

import { Button } from "@/components/ui/button"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from "@/components/ui/field"
import { Textarea } from "@/components/ui/textarea"
import { escalateIncident } from "@/features/incidents/api/incidents-api"
import type { IncidentDetail } from "@/features/incidents/model/incident.types"
import { ApiError } from "@/shared/api/api-error"

const ESCALATION_REASON_MAX_LENGTH = 1000

interface IncidentEscalationFormProps {
  incidentId: number
  onSuccess: (incident: IncidentDetail) => void
}

export function IncidentEscalationForm({
  incidentId,
  onSuccess,
}: IncidentEscalationFormProps) {
  const reasonId = useId()
  const descriptionId = `${reasonId}-description`
  const countId = `${reasonId}-count`
  const errorId = `${reasonId}-error`
  const [reason, setReason] = useState("")
  const [error, setError] = useState<string>()
  const [isSubmitting, setIsSubmitting] = useState(false)
  const errorRef = useRef<HTMLDivElement>(null)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSubmitting) {
      return
    }
    if (!reason.trim()) {
      setError("Enter a reason before escalating the incident.")
      requestAnimationFrame(() => errorRef.current?.focus())
      return
    }

    setIsSubmitting(true)
    setError(undefined)
    try {
      const updated = await escalateIncident(incidentId, reason)
      setReason("")
      onSuccess(updated)
    } catch (caught) {
      setError(
        caught instanceof ApiError
          ? (caught.fieldErrors.reason ?? caught.message)
          : "The incident could not be escalated. Try again.",
      )
      requestAnimationFrame(() => errorRef.current?.focus())
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form
      className="mt-4 rounded-lg border bg-muted/20 p-4"
      onSubmit={(event) => void submit(event)}
    >
      <Field data-invalid={error ? "true" : undefined}>
        <FieldLabel htmlFor={reasonId}>Escalation reason</FieldLabel>
        <FieldDescription id={descriptionId}>
          Explain the current impact or why additional attention is needed.
          The server assigns the next escalation level.
        </FieldDescription>
        <Textarea
          aria-describedby={[
            descriptionId,
            countId,
            error ? errorId : undefined,
          ]
            .filter(Boolean)
            .join(" ")}
          aria-invalid={error ? "true" : undefined}
          disabled={isSubmitting}
          id={reasonId}
          maxLength={ESCALATION_REASON_MAX_LENGTH}
          onChange={(event) => {
            setReason(event.target.value)
            setError(undefined)
          }}
          placeholder="Describe the impact and requested attention…"
          rows={4}
          value={reason}
        />
        <div className="flex items-start justify-between gap-3 text-xs text-muted-foreground">
          <span aria-live="polite" id={countId}>
            {reason.length.toLocaleString()} /{" "}
            {ESCALATION_REASON_MAX_LENGTH.toLocaleString()} characters
          </span>
          <span>Required</span>
        </div>
        {error ? (
          <FieldError id={errorId} ref={errorRef} tabIndex={-1}>
            {error}
          </FieldError>
        ) : null}
      </Field>
      <Button
        className="mt-3"
        disabled={isSubmitting}
        size="sm"
        type="submit"
        variant="destructive"
      >
        {isSubmitting ? "Escalating…" : "Escalate incident"}
      </Button>
    </form>
  )
}
