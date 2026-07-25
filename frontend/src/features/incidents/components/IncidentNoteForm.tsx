import { useId, useRef, useState, type FormEvent } from "react"

import { Button } from "@/components/ui/button"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from "@/components/ui/field"
import { Textarea } from "@/components/ui/textarea"
import { addIncidentNote } from "@/features/incidents/api/incidents-api"
import type { IncidentDetail } from "@/features/incidents/model/incident.types"
import { ApiError } from "@/shared/api/api-error"

const NOTE_MAX_LENGTH = 2000

interface IncidentNoteFormProps {
  incidentId: number
  onSuccess: (incident: IncidentDetail) => void
}

export function IncidentNoteForm({
  incidentId,
  onSuccess,
}: IncidentNoteFormProps) {
  const noteId = useId()
  const descriptionId = `${noteId}-description`
  const countId = `${noteId}-count`
  const errorId = `${noteId}-error`
  const [note, setNote] = useState("")
  const [error, setError] = useState<string>()
  const [isSubmitting, setIsSubmitting] = useState(false)
  const errorRef = useRef<HTMLDivElement>(null)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSubmitting) {
      return
    }
    if (!note.trim()) {
      setError("Enter a note before adding it.")
      requestAnimationFrame(() => errorRef.current?.focus())
      return
    }

    setIsSubmitting(true)
    setError(undefined)
    try {
      const updated = await addIncidentNote(incidentId, note)
      setNote("")
      onSuccess(updated)
    } catch (caught) {
      setError(
        caught instanceof ApiError
          ? (caught.fieldErrors.note ?? caught.message)
          : "The note could not be added. Try again.",
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
        <FieldLabel htmlFor={noteId}>Timeline note</FieldLabel>
        <FieldDescription id={descriptionId}>
          Record an action, decision, or observation for the response team.
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
          id={noteId}
          maxLength={NOTE_MAX_LENGTH}
          onChange={(event) => {
            setNote(event.target.value)
            setError(undefined)
          }}
          placeholder="Add operational context…"
          rows={4}
          value={note}
        />
        <div className="flex items-start justify-between gap-3 text-xs text-muted-foreground">
          <span aria-live="polite" id={countId}>
            {note.length.toLocaleString()} /{" "}
            {NOTE_MAX_LENGTH.toLocaleString()} characters
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
      >
        {isSubmitting ? "Adding note…" : "Add note"}
      </Button>
    </form>
  )
}
