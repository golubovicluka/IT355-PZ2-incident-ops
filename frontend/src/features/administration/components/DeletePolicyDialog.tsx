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
  Alert,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert"
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
import type { EscalationPolicy } from "@/features/administration/model/policy.types"
import { ApiError } from "@/shared/api/api-error"

interface DeletePolicyDialogProps {
  finalFocusRef: RefObject<HTMLElement | null>
  onDelete: (policy: EscalationPolicy) => Promise<void>
  onOpenChange: (open: boolean) => void
  open: boolean
  policy: EscalationPolicy
}

export function DeletePolicyDialog({
  finalFocusRef,
  onDelete,
  onOpenChange,
  open,
  policy,
}: DeletePolicyDialogProps) {
  const [isPending, setIsPending] = useState(false)
  const [deleteError, setDeleteError] = useState<string>()
  const errorFeedbackRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) {
      return
    }

    setIsPending(false)
    setDeleteError(undefined)
  }, [open, policy.id])

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
      await onDelete(policy)
      setIsPending(false)
      onOpenChange(false)
    } catch (error) {
      setDeleteError(
        error instanceof ApiError
          ? error.message
          : "The escalation policy could not be deleted. Try again.",
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
          <AlertDialogTitle>
            Delete {policy.managedService.name} {policy.priority} policy?
          </AlertDialogTitle>
          <AlertDialogDescription>
            This removes the SLA deadlines for this service and priority.
            Active incidents may prevent deletion. The change cannot be
            undone.
          </AlertDialogDescription>
        </AlertDialogHeader>

        {deleteError ? (
          <Alert
            ref={errorFeedbackRef}
            tabIndex={-1}
            variant="destructive"
          >
            <HugeiconsIcon icon={AlertCircleIcon} strokeWidth={2} />
            <AlertTitle>Escalation policy could not be deleted</AlertTitle>
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
            <HugeiconsIcon
              className={isPending ? "animate-spin" : undefined}
              data-icon="inline-start"
              icon={isPending ? Loading03Icon : Delete02Icon}
              strokeWidth={2}
            />
            {isPending ? "Deleting…" : "Delete policy"}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
