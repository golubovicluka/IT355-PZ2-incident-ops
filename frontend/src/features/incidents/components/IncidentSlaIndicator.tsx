import {
  AlertTriangleIcon,
  CheckCircle2Icon,
  Clock3Icon,
  CircleHelpIcon,
} from "lucide-react"

import { Badge } from "@/components/ui/badge"
import type {
  IncidentSla,
  SlaPhase,
} from "@/features/incidents/model/incident.types"

const phaseLabels: Record<SlaPhase, string> = {
  ACKNOWLEDGEMENT: "Acknowledgement",
  RESOLUTION: "Resolution",
}

function formatDeadline(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value))
}

function indicatorLabel(sla: IncidentSla) {
  if (sla.state === "NOT_CONFIGURED") {
    return "SLA not configured"
  }

  const phase = sla.phase ? phaseLabels[sla.phase] : "SLA"

  if (sla.state === "BREACHED") {
    return `${phase} breached`
  }
  if (sla.state === "MET") {
    return `${phase} met`
  }
  return `${phase} due`
}

export function IncidentSlaIndicator({ sla }: { sla: IncidentSla }) {
  const label = indicatorLabel(sla)
  const variant =
    sla.state === "BREACHED"
      ? ("destructive" as const)
      : sla.state === "ON_TRACK"
        ? ("secondary" as const)
        : ("outline" as const)
  const Icon =
    sla.state === "BREACHED"
      ? AlertTriangleIcon
      : sla.state === "MET"
        ? CheckCircle2Icon
        : sla.state === "ON_TRACK"
          ? Clock3Icon
          : CircleHelpIcon

  return (
    <span className="flex min-w-0 flex-col items-start gap-1.5">
      <Badge variant={variant}>
        <Icon data-icon="inline-start" />
        {label}
      </Badge>
      {sla.deadline ? (
        <span className="text-xs text-muted-foreground">
          {sla.state === "BREACHED" ? "Deadline" : "Due"}{" "}
          <time dateTime={sla.deadline}>{formatDeadline(sla.deadline)}</time>
        </span>
      ) : null}
    </span>
  )
}
