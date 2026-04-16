import type { ErrorBudget } from "../../types/dashboard";

interface ErrorBudgetCardProps {
  data: ErrorBudget;
}

export default function ErrorBudgetCard({ data }: ErrorBudgetCardProps) {
  const isHealthy = data.remainingPercentage > 30;
  const isWarning = data.remainingPercentage > 10 && data.remainingPercentage <= 30;

  const barColor = isHealthy
    ? "bg-success-500"
    : isWarning
      ? "bg-warning-500"
      : "bg-error-500";

  const textColor = isHealthy
    ? "text-success-600 dark:text-success-400"
    : isWarning
      ? "text-warning-600 dark:text-warning-400"
      : "text-error-600 dark:text-error-400";

  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
      <h3 className="text-lg font-semibold text-gray-800 dark:text-white/90 mb-4">
        Error Budget
      </h3>

      <div className="flex items-baseline justify-between mb-3">
        <span className={`text-2xl font-bold ${textColor}`}>
          {data.remainingPercentage}%
        </span>
        <span className="text-sm text-gray-500 dark:text-gray-400">remaining</span>
      </div>

      <div className="w-full h-3 rounded-full bg-gray-200 dark:bg-gray-700 mb-4">
        <div
          className={`h-3 rounded-full transition-all ${barColor}`}
          style={{ width: `${data.remainingPercentage}%` }}
        />
      </div>

      <div className="grid grid-cols-2 gap-4 text-sm">
        <div>
          <span className="text-gray-500 dark:text-gray-400">Burn Rate</span>
          <p className="font-semibold text-gray-800 dark:text-white/90">
            {data.burnRate}x
          </p>
        </div>
        <div>
          <span className="text-gray-500 dark:text-gray-400">Window</span>
          <p className="font-semibold text-gray-800 dark:text-white/90">
            {data.windowDays} days
          </p>
        </div>
      </div>
    </div>
  );
}
