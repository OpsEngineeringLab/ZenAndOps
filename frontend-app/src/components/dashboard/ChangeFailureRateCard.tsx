import type { ChangeManagement } from "../../types/dashboard";

interface ChangeFailureRateCardProps {
  data: ChangeManagement;
}

export default function ChangeFailureRateCard({ data }: ChangeFailureRateCardProps) {
  const isHealthy = data.changeFailureRatePercentage <= 5;

  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
      <h3 className="text-lg font-semibold text-gray-800 dark:text-white/90 mb-4">
        Change Failure Rate
      </h3>

      <div className="flex items-baseline gap-2 mb-4">
        <span
          className={`text-2xl font-bold ${
            isHealthy
              ? "text-success-600 dark:text-success-400"
              : "text-error-600 dark:text-error-400"
          }`}
        >
          {data.changeFailureRatePercentage}%
        </span>
      </div>

      <div className="grid grid-cols-2 gap-4 text-sm">
        <div>
          <span className="text-gray-500 dark:text-gray-400">Total Changes</span>
          <p className="font-semibold text-gray-800 dark:text-white/90">
            {data.totalChanges}
          </p>
        </div>
        <div>
          <span className="text-gray-500 dark:text-gray-400">Failed Changes</span>
          <p className="font-semibold text-gray-800 dark:text-white/90">
            {data.failedChanges}
          </p>
        </div>
      </div>
    </div>
  );
}
