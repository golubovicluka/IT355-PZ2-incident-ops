import { ShieldCheckIcon } from "lucide-react"
import type { ReactNode } from "react"
import { Link } from "react-router-dom"

import { Badge } from "@/components/ui/badge"
import { buttonVariants } from "@/components/ui/button"
import {
  Card,
  CardAction,
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
  footerDescription?: string
  children: ReactNode
  alternateAction: {
    label: string
    to: string
  }
}

export function AuthenticationCard({
  title,
  description,
  footerDescription = "This screen validates details locally and does not send them to an API.",
  children,
  alternateAction,
}: AuthenticationCardProps) {
  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <Badge variant="secondary">
          <ShieldCheckIcon data-icon="inline-start" />
          IncidentOps
        </Badge>
        <CardAction>
          <Link
            className={buttonVariants({ size: "sm", variant: "link" })}
            to={alternateAction.to}
          >
            {alternateAction.label}
          </Link>
        </CardAction>
        <CardTitle>
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        </CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent>{children}</CardContent>
      <CardFooter className="flex-col items-start gap-4">
        <Separator />
        <p className="text-xs leading-relaxed text-muted-foreground">
          {footerDescription}
        </p>
      </CardFooter>
    </Card>
  )
}
