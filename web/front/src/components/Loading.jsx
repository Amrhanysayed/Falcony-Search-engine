import { Loader } from "lucide-react";

const Loading = () => {
  return (
    <div className="flex items-center justify-center h-40">
      <Loader className="animate-spin w-8 h-8 text-blue-500" />
    </div>
  );
};

export default Loading;
