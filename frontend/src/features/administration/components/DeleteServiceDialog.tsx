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
import type { ManagedService } from "@/features/administration/model/service.types"
import { ApiError } from "@/shared/api/api-error"

interface DeleteServiceDialogProps {
  finalFocusRef: RefObject<HTMLElement | null>
  onDelete: (service: ManagedService) => Promise<void>
  onOpenChange: (open: boolean) => void
  open: boolean
  service: ManagedService
}

export function DeleteServiceDialog({
  finalFocusRef,
  onDelete,
  onOpenChange,
  open,
  service,
}: DeleteServiceDialogProps) {
  const [isPending, setIsPending] = useState(false)
  const [deleteError, setDeleteError] = useState<string>()
  const errorFeedbackRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) {
      return
    }

    setIsPending(false)
    setDeleteError(undefined)
  }, [open, service.id])

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
      await onDelete(service)
      setIsPending(false)
      onOpenChange(false)
    } catch (error) {
      setDeleteError(
        error instanceof ApiError
          ? error.message
          : "The managed service could not be deleted. Try again.",
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
          <AlertDialogTitle>Delete {service.name}?</AlertDialogTitle>
          <AlertDialogDescription>
            This removes the service from incident forms and filters. The
            change cannot be undone.
          </AlertDialogDescription>
        </AlertDialogHeader>

        {deleteError ? (
          <Alert
            ref={errorFeedbackRef}
            tabIndex={-1}
            variant="destructive"
          >
            <HugeiconsIcon icon={AlertCircleIcon} strokeWidth={2} />
            <AlertTitle>Managed service could not be deleted</AlertTitle>
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
            {isPending ? "Deleting…" : "Delete service"}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
