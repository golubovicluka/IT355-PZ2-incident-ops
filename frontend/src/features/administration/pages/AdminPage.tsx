import {
  CatalogueIcon,
  ShieldUserIcon,
  UserGroupIcon,
} from "@hugeicons/core-free-icons"
import { HugeiconsIcon } from "@hugeicons/react"

import { Badge } from "@/components/ui/badge"
import { buttonVariants } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { TeamManagementSection } from "@/features/administration/components/TeamManagementSection"

export function AdminPage() {
  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader className="gap-3">
          <Badge variant="secondary">
            <HugeiconsIcon
              data-icon="inline-start"
              icon={ShieldUserIcon}
              strokeWidth={2}
            />
            Administrator
          </Badge>
          <CardTitle>
            <h1 className="text-3xl font-semibold tracking-tight">
              Administration
            </h1>
          </CardTitle>
          <CardDescription className="max-w-2xl">
            Manage the operational catalog available to IncidentOps. These
            controls are restricted to authenticated administrators.
          </CardDescription>
        </CardHeader>
      </Card>

      <div className="grid gap-6 lg:grid-cols-[15rem_minmax(0,1fr)]">
        <Card className="h-fit" size="sm">
          <CardHeader>
            <CardTitle>
              <span className="flex items-center gap-2">
                <HugeiconsIcon icon={CatalogueIcon} strokeWidth={2} />
                Catalog
              </span>
            </CardTitle>
            <CardDescription>
              Choose an administrative resource.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <a
              className={buttonVariants({
                className: "w-full justify-start",
                variant: "secondary",
              })}
              href="#teams"
            >
              <HugeiconsIcon
                data-icon="inline-start"
                icon={UserGroupIcon}
                strokeWidth={2}
              />
              Teams
            </a>
          </CardContent>
        </Card>

        <TeamManagementSection />
      </div>
    </div>
  )
}
