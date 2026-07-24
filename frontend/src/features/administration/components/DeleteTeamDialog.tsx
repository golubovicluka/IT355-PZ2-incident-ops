import {
  AlertCircleIcon,
  Delete02Icon,
  Loading03Icon,
} from "@hugeicons/core-free-icons"
import { HugeiconsIcon } from "@hugeicons/react"
import {
  useEffect,
  useRef,
  useState,
  type RefObject,
} from "react"

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogMedia,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import {
  Alert,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert"
import type { Team } from "@/features/administration/model/team.types"
import { ApiError } from "@/shared/api/api-error"

interface DeleteTeamDialogProps {
  finalFocusRef: RefObject<HTMLElement | null>
  onDelete: (team: Team) => Promise<void>
  onOpenChange: (open: boolean) => void
  open: boolean
  team: Team
}

export function DeleteTeamDialog({
  finalFocusRef,
  onDelete,
  onOpenChange,
  open,
  team,
}: DeleteTeamDialogProps) {
  const [isPending, setIsPending] = useState(false)
  const [deleteError, setDeleteError] = useState<string>()
  const errorFeedbackRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) {
      return
    }

    setIsPending(false)
    setDeleteError(undefined)
  }, [open, team.id])

  useEffect(() => {
    if (deleteError) {
      requestAnimationFrame(() => errorFeedbackRef.current?.focus())
    }
  }, [deleteError])

  function handleOpenChange(nextOpen: boolean) {
    if (!isPending) {
      onOpenChange(nextOpen)
    }
  }

  async function handleDelete() {
    if (isPending) {
      return
    }

    setIsPending(true)
    setDeleteError(undefined)

    try {
      await onDelete(team)
      setIsPending(false)
      onOpenChange(false)
    } catch (error) {
      setDeleteError(
        error instanceof ApiError
          ? error.message
          : "The team could not be deleted. Try again.",
      )
      setIsPending(false)
    }
  }

  return (
    <AlertDialog onOpenChange={handleOpenChange} open={open}>
      <AlertDialogContent finalFocus={finalFocusRef}>
        <AlertDialogHeader>
          <AlertDialogMedia>
            <HugeiconsIcon icon={Delete02Icon} strokeWidth={2} />
          </AlertDialogMedia>
          <AlertDialogTitle>Delete {team.name}?</AlertDialogTitle>
          <AlertDialogDescription>
            This removes the team from the administrative catalog. The change
            cannot be undone.
          </AlertDialogDescription>
        </AlertDialogHeader>

        {deleteError ? (
          <Alert
            ref={errorFeedbackRef}
            tabIndex={-1}
            variant="destructive"
          >
            <HugeiconsIcon icon={AlertCircleIcon} strokeWidth={2} />
            <AlertTitle>Team could not be deleted</AlertTitle>
            <AlertDescription>{deleteError}</AlertDescription>
          </Alert>
        ) : null}

        <AlertDialogFooter>
          <AlertDialogCancel disabled={isPending}>Cancel</AlertDialogCancel>
          <AlertDialogAction
            disabled={isPending}
            onClick={handleDelete}
            variant="destructive"
          >
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
                icon={Delete02Icon}
                strokeWidth={2}
              />
            )}
            {isPending ? "Deleting…" : "Delete team"}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
