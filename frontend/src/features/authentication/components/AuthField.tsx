import type { ComponentProps } from "react"

import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"

interface AuthFieldProps extends Omit<ComponentProps<typeof Input>, "id"> {
  id: string
  label: string
  description?: string
  error?: string
}

export function AuthField({
  id,
  label,
  description,
  error,
  ...inputProps
}: AuthFieldProps) {
  const descriptionId = description ? `${id}-description` : undefined
  const errorId = error ? `${id}-error` : undefined
  const describedBy = [descriptionId, errorId].filter(Boolean).join(" ")

  return (
    <Field data-invalid={Boolean(error)}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <Input
        aria-describedby={describedBy || undefined}
        aria-invalid={Boolean(error) || undefined}
        id={id}
        {...inputProps}
      />
      {description ? (
        <FieldDescription id={descriptionId}>{description}</FieldDescription>
      ) : null}
      {error ? <FieldError id={errorId}>{error}</FieldError> : null}
    </Field>
  )
}
