import { AlertIcon, BoxIconLine, CheckCircleIcon, PieChartIcon } from "../../icons";
import type { ExecutiveSummary } from "../../types/dashboard";

interface ExecutiveSummaryCardsProps {
  data: ExecutiveSummary;
}

export default function ExecutiveSummaryCards({ data }: ExecutiveSummaryCardsProps) {
  const metrics = [
    {
      label: "Total Open Tickets",
      value: data.totalOpenTickets.toLocaleString(),
      icon: <BoxIconLine className="text-gray-800 size-6 dark:text-white/90" />,
      iconBg: "bg-gray-100 dark:bg-gray-800",
    },
    {
      label: "Critical Incidents",
      value: data.criticalIncidents.toString(),
      icon: <AlertIcon className="text-error-600 size-6 dark:text-error-400" />,
      iconBg: "bg-error-50 dark:bg-error-500/15",
    },
    {
      label: "Overall Availability",
      value: `${data.overallAvailability}%`,
      icon: <CheckCircleIcon className="text-success-600 size-6 dark:text-success-400" />,
      iconBg: "bg-success-50 dark:bg-success-500/15",
    },
    {
      label: "Error Budget Remaining",
      value: `${data.errorBudgetRemaining}%`,
      icon: <PieChartIcon className="text-brand-500 size-6 dark:text-brand-400" />,
      iconBg: "bg-brand-50 dark:bg-brand-500/15",
    },
  ];

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4 md:gap-6">
      {metrics.map((metric) => (
        <div
          key={metric.label}
          className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6"
        >
          <div className={`flex items-center justify-center w-12 h-12 rounded-xl ${metric.iconBg}`}>
            {metric.icon}
          </div>
          <div className="mt-5">
            <span className="text-sm text-gray-500 dark:text-gray-400">
              {metric.label}
            </span>
            <h4 className="mt-2 font-bold text-gray-800 text-title-sm dark:text-white/90">
              {metric.value}
            </h4>
          </div>
        </div>
      ))}
    </div>
  );
}
