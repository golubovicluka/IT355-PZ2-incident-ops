import { ShieldCheckIcon } from "lucide-react"
import type { ReactNode } from "react"

import { Badge } from "@/components/ui/badge"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Separator } from "@/components/ui/separator"

interface AuthenticationCardProps {
  title: string
  description: string
  children: ReactNode
}

export function AuthenticationCard({
  title,
  description,
  children,
}: AuthenticationCardProps) {
  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <Badge variant="secondary">
          <ShieldCheckIcon data-icon="inline-start" />
          IncidentOps
        </Badge>
        <CardTitle>
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        </CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent>{children}</CardContent>
      <CardFooter className="flex-col items-start gap-4">
        <Separator />
        <p className="text-xs leading-relaxed text-muted-foreground">
          Credentials stay in this form until the authentication API is
          connected.
        </p>
      </CardFooter>
    </Card>
  )
}
