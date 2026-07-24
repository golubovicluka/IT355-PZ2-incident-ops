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
import type { Team } from "@/features/administration/model/team.types"
import {
  normalizeTeamName,
  TEAM_NAME_MAX_LENGTH,
  validateTeamName,
} from "@/features/administration/model/team.validation"
import { ApiError } from "@/shared/api/api-error"

interface SharedTeamFormDialogProps {
  finalFocusRef: RefObject<HTMLElement | null>
  onOpenChange: (open: boolean) => void
  onSubmit: (name: string) => Promise<void>
  open: boolean
}

type TeamFormDialogProps = SharedTeamFormDialogProps &
  (
    | {
        mode: "create"
        team?: never
      }
    | {
        mode: "edit"
        team: Team
      }
  )

export function TeamFormDialog({
  finalFocusRef,
  mode,
  onOpenChange,
  onSubmit,
  open,
  team,
}: TeamFormDialogProps) {
  const initialName = mode === "edit" ? team.name : ""
  const inputId = mode === "edit" ? `team-name-${team.id}` : "new-team-name"
  const descriptionId = `${inputId}-description`
  const errorId = `${inputId}-error`
  const [name, setName] = useState(initialName)
  const [nameError, setNameError] = useState<string>()
  const [submitError, setSubmitError] = useState<string>()
  const [isPending, setIsPending] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)
  const errorFeedbackRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) {
      return
    }

    setName(initialName)
    setNameError(undefined)
    setSubmitError(undefined)
    setIsPending(false)
  }, [initialName, open])

  useEffect(() => {
    if (!submitError) {
      return
    }

    requestAnimationFrame(() => {
      if (nameError) {
        inputRef.current?.focus()
      } else {
        errorFeedbackRef.current?.focus()
      }
    })
  }, [nameError, submitError])

  function handleOpenChange(nextOpen: boolean) {
    if (!isPending) {
      onOpenChange(nextOpen)
    }
  }

  function handleNameChange(event: ChangeEvent<HTMLInputElement>) {
    setName(event.target.value)
    setNameError(undefined)
    setSubmitError(undefined)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (isPending) {
      return
    }

    const validationError = validateTeamName(name)
    setNameError(validationError)

    if (validationError) {
      requestAnimationFrame(() => inputRef.current?.focus())
      return
    }

    setIsPending(true)
    setSubmitError(undefined)

    try {
      await onSubmit(normalizeTeamName(name))
      setIsPending(false)
      onOpenChange(false)
    } catch (error) {
      if (error instanceof ApiError) {
        setNameError(error.fieldErrors.name)
        setSubmitError(error.message)
      } else {
        setSubmitError("The team could not be saved. Try again.")
      }
      setIsPending(false)
    }
  }

  const isEditing = mode === "edit"
  const title = isEditing ? "Edit team" : "Create team"
  const description = isEditing
    ? "Update the name used throughout the incident catalog."
    : "Add a team that can own and coordinate incident work."

  return (
    <Dialog onOpenChange={handleOpenChange} open={open}>
      <DialogContent
        finalFocus={finalFocusRef}
        initialFocus={inputRef}
        showCloseButton={!isPending}
      >
        <form
          className="flex flex-col gap-4"
          noValidate
          onSubmit={handleSubmit}
        >
          <DialogHeader>
            <DialogTitle>{title}</DialogTitle>
            <DialogDescription>{description}</DialogDescription>
          </DialogHeader>

          <FieldGroup>
            <Field
              data-disabled={isPending || undefined}
              data-invalid={Boolean(nameError)}
            >
              <FieldLabel htmlFor={inputId}>Team name</FieldLabel>
              <Input
                aria-describedby={`${descriptionId}${
                  nameError ? ` ${errorId}` : ""
                }`}
                aria-invalid={Boolean(nameError) || undefined}
                autoComplete="off"
                disabled={isPending}
                id={inputId}
                name="teamName"
                onChange={handleNameChange}
                placeholder="e.g. Platform Operations"
                ref={inputRef}
                value={name}
              />
              <FieldDescription id={descriptionId}>
                Use a clear operational name, up to {TEAM_NAME_MAX_LENGTH}{" "}
                characters.
              </FieldDescription>
              {nameError ? (
                <FieldError id={errorId}>{nameError}</FieldError>
              ) : null}
            </Field>
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
                  ? "Team could not be updated"
                  : "Team could not be created"}
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
              {isPending ? (
                <HugeiconsIcon
                  className="animate-spin"
                  data-icon="inline-start"
                  icon={Loading03Icon}
                  strokeWidth={2}
                />
              ) : (
                <HugeiconsIcon
                  data-icon="inline-start"
                  icon={isEditing ? PencilEdit01Icon : Add01Icon}
                  strokeWidth={2}
                />
              )}
              {isPending
                ? isEditing
                  ? "Saving…"
                  : "Creating…"
                : isEditing
                  ? "Save changes"
                  : "Create team"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
