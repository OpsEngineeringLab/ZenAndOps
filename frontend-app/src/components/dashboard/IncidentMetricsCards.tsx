import { ArrowUpIcon, ArrowDownIcon } from "../../icons";
import Badge from "../ui/badge/Badge";
import type { IncidentMetrics, TrendDirection } from "../../types/dashboard";

interface IncidentMetricsCardsProps {
  data: IncidentMetrics;
}

function TrendBadge({ trend }: { trend: TrendDirection }) {
  // For MTTR/MTTD, DOWN is good (faster recovery/detection), UP is bad
  if (trend === "STABLE") {
    return (
      <Badge color="info">
        &#8212; Stable
      </Badge>
    );
  }

  const isGood = trend === "DOWN";
  return (
    <Badge color={isGood ? "success" : "error"}>
      {trend === "UP" ? <ArrowUpIcon /> : <ArrowDownIcon />}
      {trend === "DOWN" ? "Improving" : "Degrading"}
    </Badge>
  );
}

export default function IncidentMetricsCards({ data }: IncidentMetricsCardsProps) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:gap-6">
      <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
        <div className="flex items-end justify-between">
          <div>
            <span className="text-sm text-gray-500 dark:text-gray-400">
              MTTR (Mean Time to Recovery)
            </span>
            <h4 className="mt-2 font-bold text-gray-800 text-title-sm dark:text-white/90">
              {data.mttrMinutes} min
            </h4>
          </div>
          <TrendBadge trend={data.mttrTrend} />
        </div>
      </div>

      <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
        <div className="flex items-end justify-between">
          <div>
            <span className="text-sm text-gray-500 dark:text-gray-400">
              MTTD (Mean Time to Detect)
            </span>
            <h4 className="mt-2 font-bold text-gray-800 text-title-sm dark:text-white/90">
              {data.mttdMinutes} min
            </h4>
          </div>
          <TrendBadge trend={data.mttdTrend} />
        </div>
      </div>
    </div>
  );
}
