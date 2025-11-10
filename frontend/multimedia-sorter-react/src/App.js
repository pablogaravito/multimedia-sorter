import React, { useState, useEffect } from "react";
import {
  ChevronLeft,
  FolderPlus,
  Play,
  SkipForward,
  Save,
  AlertCircle,
  CheckCircle,
  Loader,
} from "lucide-react";

const API_BASE = "http://localhost:8080/api";

export default function MultimediaSorter() {
  const [sourcePath, setSourcePath] = useState("");
  const [images, setImages] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [destinations, setDestinations] = useState([]);
  const [classifications, setClassifications] = useState({});
  const [newDestName, setNewDestName] = useState("");
  const [newDestKey, setNewDestKey] = useState("");
  const [newDestPath, setNewDestPath] = useState("");
  const [isStarted, setIsStarted] = useState(false);
  const [feedback, setFeedback] = useState("");
  const [loading, setLoading] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [sessionLoaded, setSessionLoaded] = useState(false);
  const [zoom, setZoom] = useState(1);
  const [imagePosition, setImagePosition] = useState({ x: 0, y: 0 });
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [isMouseOverImage, setIsMouseOverImage] = useState(false);

  const showFeedback = (msg, duration = 2000) => {
    setFeedback(msg);
    setTimeout(() => setFeedback(""), duration);
  };

  useEffect(() => {
    loadSavedDestinations();
  }, []);

  const loadSavedDestinations = async () => {
    try {
      const response = await fetch(`${API_BASE}/destinations`);
      if (!response.ok) return;
      const data = await response.json();
      if (data && data.length > 0) {
        setDestinations(data);
        showFeedback(`Loaded ${data.length} saved destinations`, 2000);
      }
    } catch (error) {
      console.error("Error loading destinations:", error);
    }
  };

  const saveDestinationsToBackend = async (dests) => {
    try {
      await fetch(`${API_BASE}/destinations`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(dests),
      });
    } catch (error) {
      console.error("Error saving destinations:", error);
    }
  };

  const loadImages = async () => {
    if (!sourcePath.trim()) {
      showFeedback("Please enter a source path");
      return;
    }
    setLoading(true);
    try {
      const response = await fetch(
        `${API_BASE}/images?sourcePath=${encodeURIComponent(sourcePath)}`
      );
      if (!response.ok) throw new Error("Failed to load images");
      const data = await response.json();
      setImages(data);
      showFeedback(`Loaded ${data.length} images`);
      await loadSession();
    } catch (error) {
      showFeedback("Error loading images: " + error.message, 4000);
    } finally {
      setLoading(false);
    }
  };

  const loadSession = async () => {
    try {
      const response = await fetch(
        `${API_BASE}/session?sourcePath=${encodeURIComponent(sourcePath)}`
      );
      if (!response.ok) return;
      const session = await response.json();
      if (session.destinations && session.destinations.length > 0) {
        setDestinations(session.destinations);
        setClassifications(session.classifications || {});
        setCurrentIndex(session.currentIndex || 0);
        setSessionLoaded(true);
        showFeedback("Previous session restored", 3000);
      }
    } catch (error) {
      console.log("No previous session found");
    }
  };

  const saveSession = async () => {
    if (!hasUnsavedChanges) return;
    try {
      const session = {
        sourcePath,
        destinations,
        classifications,
        currentIndex,
        lastSaved: Date.now(),
      };
      await fetch(`${API_BASE}/session`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(session),
      });
      setHasUnsavedChanges(false);
      showFeedback("Progress saved");
    } catch (error) {
      showFeedback("Error saving progress", 4000);
    }
  };

  const addDestination = () => {
    if (!newDestName.trim() || !newDestKey.trim() || !newDestPath.trim()) {
      showFeedback("Please fill all fields");
      return;
    }
    const key = newDestKey.toLowerCase();
    if (destinations.some((d) => d.key === key)) {
      showFeedback("Key already used");
      return;
    }
    const newDests = [
      ...destinations,
      { name: newDestName, key, path: newDestPath },
    ];
    setDestinations(newDests);
    saveDestinationsToBackend(newDests);
    setNewDestName("");
    setNewDestKey("");
    setNewDestPath("");
    showFeedback(`Added: ${newDestName} (${key})`);
  };

  const removeDestination = (key) => {
    const dest = destinations.find((d) => d.key === key);
    const confirmed = window.confirm(
      `Remove destination "${dest.name}" (${dest.key})?\n\nPath: ${dest.path}\n\nThis won't delete any files, just removes it from your saved destinations.`
    );
    if (!confirmed) return;
    const newDests = destinations.filter((d) => d.key !== key);
    setDestinations(newDests);
    saveDestinationsToBackend(newDests);
    showFeedback("Destination removed");
  };

  const openImageInDefaultApp = async () => {
    if (!currentImage) return;
    try {
      await fetch(
        `${API_BASE}/open-file?path=${encodeURIComponent(currentImage.path)}`
      );
      showFeedback("Opened in default app");
    } catch (error) {
      showFeedback("Could not open file", 3000);
    }
  };

  const classifyImage = (destName) => {
    if (currentIndex >= images.length) return;
    const image = images[currentIndex];
    const newClassifications = { ...classifications, [image.path]: destName };
    setClassifications(newClassifications);
    setHasUnsavedChanges(true);
    showFeedback(`→ ${destName}`);
    if (currentIndex < images.length - 1) {
      setCurrentIndex(currentIndex + 1);
    }
    setZoom(1);
    setImagePosition({ x: 0, y: 0 });
  };

  const skipImage = () => {
    if (currentIndex < images.length - 1) {
      setCurrentIndex(currentIndex + 1);
      showFeedback("Skipped");
      setZoom(1);
      setImagePosition({ x: 0, y: 0 });
    }
  };

  const goToPrevious = () => {
    if (currentIndex > 0) {
      setCurrentIndex(currentIndex - 1);
      setZoom(1);
      setImagePosition({ x: 0, y: 0 });
    }
  };

  const resetToStart = () => {
    setIsStarted(false);
    setImages([]);
    setCurrentIndex(0);
    setClassifications({});
    setSessionLoaded(false);
    setSourcePath("");
    setZoom(1);
    setImagePosition({ x: 0, y: 0 });
    setHasUnsavedChanges(false);
    showFeedback("Ready for new session");
  };

  const finishSorting = async () => {
    if (Object.keys(classifications).length === 0) {
      showFeedback("No images classified yet");
      return;
    }
    const confirmed = window.confirm(
      `Ready to move files?\n\n` +
        `- ${
          Object.keys(classifications).length
        } images will be copied, verified, and deleted from source\n` +
        `- This operation uses SHA-256 hash verification\n` +
        `- Duplicates will be detected and skipped\n\n` +
        `Continue?`
    );
    if (!confirmed) return;
    setProcessing(true);
    try {
      const response = await fetch(`${API_BASE}/sort`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sourcePath, destinations, classifications }),
      });
      const result = await response.json();
      if (result.success) {
        // Delete the session file since we're done
        try {
          await fetch(
            `${API_BASE}/session?sourcePath=${encodeURIComponent(sourcePath)}`,
            {
              method: "DELETE",
            }
          );
        } catch (e) {
          console.log("Could not delete session file");
        }
        alert(`Success!\n\n${result.message}`);
        showFeedback("All files processed successfully!", 3000);
      } else {
        alert(`Completed with issues:\n\n${result.message}`);
      }
    } catch (error) {
      alert("Error during file operations: " + error.message);
    } finally {
      setProcessing(false);
    }
  };

  useEffect(() => {
    if (!isStarted) return;
    const handleKeyPress = (e) => {
      if (processing) return;
      const key = e.key.toLowerCase();
      if (key === "arrowleft") {
        goToPrevious();
      } else if (key === "arrowright" || key === " ") {
        e.preventDefault();
        skipImage();
      } else if (key === "s" && e.ctrlKey) {
        e.preventDefault();
        saveSession();
      } else {
        const dest = destinations.find((d) => d.key === key);
        if (dest) classifyImage(dest.name);
      }
    };
    window.addEventListener("keydown", handleKeyPress);
    return () => window.removeEventListener("keydown", handleKeyPress);
  }, [isStarted, currentIndex, destinations, processing, classifications]);

  useEffect(() => {
    if (!isStarted || processing) return;
    const interval = setInterval(() => {
      if (hasUnsavedChanges) saveSession();
    }, 30000);
    return () => clearInterval(interval);
  }, [
    isStarted,
    hasUnsavedChanges,
    classifications,
    sourcePath,
    destinations,
    currentIndex,
    processing,
  ]);

  const currentImage = images[currentIndex];
  const progress =
    images.length > 0 ? ((currentIndex + 1) / images.length) * 100 : 0;
  const classifiedCount = Object.keys(classifications).length;

  // Global wheel event handler to prevent page scroll when over image
  useEffect(() => {
    const handleGlobalWheel = (e) => {
      if (isMouseOverImage && currentImage) {
        e.preventDefault();
      }
    };

    // Use passive: false to allow preventDefault
    window.addEventListener("wheel", handleGlobalWheel, { passive: false });
    return () => window.removeEventListener("wheel", handleGlobalWheel);
  }, [isMouseOverImage, currentImage]);

  const handleImageWheel = (e) => {
    if (!currentImage) return;

    const container = e.currentTarget;
    const rect = container.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    const xPercent = x / rect.width;
    const yPercent = y / rect.height;

    // Zoom speed - smaller value = smoother zoom
    const delta = e.deltaY > 0 ? -0.15 : 0.15;
    const newZoom = Math.min(Math.max(zoom + delta, 1), 5);

    if (newZoom === 1) {
      setZoom(1);
      setImagePosition({ x: 0, y: 0 });
    } else {
      const zoomDiff = newZoom - zoom;
      setZoom(newZoom);
      setImagePosition((prev) => ({
        x: prev.x - (xPercent - 0.5) * zoomDiff * rect.width,
        y: prev.y - (yPercent - 0.5) * zoomDiff * rect.height,
      }));
    }
  };

  const handleImageDrag = (e) => {
    if (zoom <= 1) return;
    e.preventDefault();
    const startX = e.clientX - imagePosition.x;
    const startY = e.clientY - imagePosition.y;
    const handleMouseMove = (moveEvent) => {
      setImagePosition({
        x: moveEvent.clientX - startX,
        y: moveEvent.clientY - startY,
      });
    };
    const handleMouseUp = () => {
      document.removeEventListener("mousemove", handleMouseMove);
      document.removeEventListener("mouseup", handleMouseUp);
    };
    document.addEventListener("mousemove", handleMouseMove);
    document.addEventListener("mouseup", handleMouseUp);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 text-white p-6">
      <div className="max-w-6xl mx-auto">
        <h1 className="text-4xl font-bold mb-2 text-center bg-gradient-to-r from-blue-400 to-purple-400 bg-clip-text text-transparent">
          Multimedia Sorter Pro
        </h1>
        <p className="text-center text-slate-400 mb-8">
          Organize recovered images with Java backend + hash verification
        </p>

        {!isStarted ? (
          <div className="space-y-6">
            <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
              <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
                <FolderPlus size={20} />
                1. Source Directory
              </h2>
              <div className="flex gap-3">
                <input
                  type="text"
                  placeholder="e.g., C:\RecoveredFiles\jpg\1"
                  value={sourcePath}
                  onChange={(e) => setSourcePath(e.target.value)}
                  className="flex-1 p-3 bg-slate-700 rounded border border-slate-600 text-white"
                  onKeyPress={(e) => e.key === "Enter" && loadImages()}
                />
                <button
                  onClick={loadImages}
                  disabled={loading}
                  className="px-6 py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-slate-600 rounded font-semibold transition flex items-center gap-2"
                >
                  {loading ? (
                    <Loader size={20} className="animate-spin" />
                  ) : (
                    "Load"
                  )}
                </button>
              </div>
              {images.length > 0 && (
                <p className="mt-2 text-green-400 flex items-center gap-2">
                  <CheckCircle size={16} />
                  {images.length} images loaded
                </p>
              )}
              {sessionLoaded && (
                <p className="mt-2 text-yellow-400 flex items-center gap-2">
                  <AlertCircle size={16} />
                  Previous session restored ({classifiedCount} already
                  classified)
                </p>
              )}
            </div>

            <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
              <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
                <FolderPlus size={20} />
                2. Destination Folders
                {destinations.length > 0 && (
                  <span className="text-sm text-slate-400 ml-auto">
                    ({destinations.length} saved)
                  </span>
                )}
              </h2>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-4">
                <input
                  type="text"
                  placeholder="Folder name (e.g., Family)"
                  value={newDestName}
                  onChange={(e) => setNewDestName(e.target.value)}
                  className="p-3 bg-slate-700 rounded border border-slate-600 text-white"
                />
                <input
                  type="text"
                  placeholder="Key (e.g., f)"
                  value={newDestKey}
                  onChange={(e) => setNewDestKey(e.target.value)}
                  maxLength={1}
                  className="p-3 bg-slate-700 rounded border border-slate-600 text-white text-center"
                />
                <input
                  type="text"
                  placeholder="Path (e.g., C:\Sorted\Family)"
                  value={newDestPath}
                  onChange={(e) => setNewDestPath(e.target.value)}
                  className="p-3 bg-slate-700 rounded border border-slate-600 text-white"
                />
              </div>
              <button
                onClick={addDestination}
                className="w-full px-6 py-3 bg-blue-600 hover:bg-blue-700 rounded font-semibold transition"
              >
                Add Destination
              </button>

              {destinations.length > 0 && (
                <div className="mt-4 space-y-2">
                  {destinations.map((dest) => (
                    <div
                      key={dest.key}
                      className="flex items-center justify-between p-3 bg-slate-700 rounded"
                    >
                      <div className="flex items-center gap-3">
                        <span className="px-3 py-1 bg-slate-600 rounded font-mono font-bold">
                          {dest.key}
                        </span>
                        <div>
                          <div className="font-semibold">{dest.name}</div>
                          <div className="text-sm text-slate-400">
                            {dest.path}
                          </div>
                        </div>
                      </div>
                      <button
                        onClick={() => removeDestination(dest.key)}
                        className="p-2 hover:bg-slate-600 rounded transition text-red-400"
                      >
                        Remove
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {images.length > 0 && destinations.length > 0 && (
              <button
                onClick={() => setIsStarted(true)}
                className="w-full p-4 bg-gradient-to-r from-green-600 to-blue-600 hover:from-green-700 hover:to-blue-700 rounded-lg font-bold text-lg flex items-center justify-center gap-2 transition"
              >
                <Play size={24} />
                Start Sorting
              </button>
            )}
          </div>
        ) : (
          <div className="space-y-6">
            <div className="bg-slate-800 rounded-lg p-4 border border-slate-700">
              <div className="flex justify-between text-sm mb-2">
                <span>
                  Progress: {currentIndex + 1} / {images.length}
                </span>
                <span>Classified: {classifiedCount}</span>
              </div>
              <div className="w-full bg-slate-700 rounded-full h-2">
                <div
                  className="bg-gradient-to-r from-blue-500 to-purple-500 h-2 rounded-full transition-all"
                  style={{ width: `${progress}%` }}
                />
              </div>
            </div>

            {currentImage && (
              <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
                <div
                  className="flex justify-center mb-4 overflow-hidden relative bg-slate-900 rounded"
                  style={{
                    height: "400px",
                    cursor: zoom > 1 ? "grab" : "default",
                  }}
                  onWheel={handleImageWheel}
                  onMouseEnter={() => setIsMouseOverImage(true)}
                  onMouseLeave={() => setIsMouseOverImage(false)}
                >
                  <img
                    src={`${API_BASE}/image?path=${encodeURIComponent(
                      currentImage.path
                    )}`}
                    alt="Current"
                    className="max-h-96 rounded object-contain select-none"
                    style={{
                      transform: `scale(${zoom}) translate(${
                        imagePosition.x / zoom
                      }px, ${imagePosition.y / zoom}px)`,
                      transformOrigin: "center center",
                      transition: "transform 0.1s ease-out",
                      cursor: zoom > 1 ? "grab" : "default",
                    }}
                    onMouseDown={handleImageDrag}
                    draggable={false}
                  />
                  {isMouseOverImage && (
                    <div className="absolute top-2 left-2 bg-black bg-opacity-60 text-white text-xs px-2 py-1 rounded">
                      Scroll to zoom
                    </div>
                  )}
                </div>
                <div className="flex justify-between items-center">
                  <p className="text-slate-400">{currentImage.name}</p>
                  <button
                    onClick={openImageInDefaultApp}
                    className="px-3 py-2 bg-blue-600 hover:bg-blue-700 rounded text-sm font-semibold transition"
                  >
                    Open in App
                  </button>
                  {zoom > 1 && (
                    <p className="text-sm text-blue-400">
                      Zoom: {Math.round(zoom * 100)}% • Drag to pan
                    </p>
                  )}
                </div>
              </div>
            )}

            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              {destinations.map((dest) => (
                <button
                  key={dest.key}
                  onClick={() => classifyImage(dest.name)}
                  disabled={processing}
                  className="p-4 bg-blue-600 hover:bg-blue-700 disabled:bg-slate-600 rounded-lg font-semibold transition flex flex-col items-center gap-1"
                >
                  <span className="text-2xl font-mono">{dest.key}</span>
                  <span className="text-sm">{dest.name}</span>
                </button>
              ))}
            </div>

            <div className="flex gap-3">
              <button
                onClick={goToPrevious}
                disabled={currentIndex === 0 || processing}
                className="flex-1 p-3 bg-slate-700 hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed rounded font-semibold transition flex items-center justify-center gap-2"
              >
                <ChevronLeft size={20} />
                Previous
              </button>
              <button
                onClick={skipImage}
                disabled={processing}
                className="flex-1 p-3 bg-slate-700 hover:bg-slate-600 disabled:opacity-50 rounded font-semibold transition flex items-center justify-center gap-2"
              >
                Skip
                <SkipForward size={20} />
              </button>
            </div>

            <div className="flex gap-3">
              <button
                onClick={saveSession}
                disabled={processing || !hasUnsavedChanges}
                className="flex-1 p-3 bg-yellow-600 hover:bg-yellow-700 disabled:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed rounded font-semibold transition flex items-center justify-center gap-2"
              >
                <Save size={20} />
                {hasUnsavedChanges ? "Save Progress" : "No Changes"}
              </button>
              <button
                onClick={finishSorting}
                disabled={processing || classifiedCount === 0}
                className="flex-1 p-3 bg-green-600 hover:bg-green-700 disabled:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed rounded font-semibold transition flex items-center justify-center gap-2"
              >
                {processing ? (
                  <Loader size={20} className="animate-spin" />
                ) : (
                  "Finish & Move Files"
                )}
              </button>
            </div>

            {!processing &&
              currentIndex >= images.length - 1 &&
              classifiedCount > 0 && (
                <div className="bg-green-900 border border-green-700 rounded-lg p-4">
                  <p className="font-semibold text-center">
                    All images reviewed! 🎉
                  </p>
                  <p className="text-sm text-green-300 text-center mb-3">
                    Click "Finish & Move Files" to complete the operation
                  </p>
                </div>
              )}

            {processing && (
              <div className="bg-blue-900 border border-blue-700 rounded-lg p-4 text-center">
                <div className="flex items-center justify-center gap-2 mb-2">
                  <Loader size={20} className="animate-spin" />
                  <p className="font-semibold">Processing files...</p>
                </div>
                <p className="text-sm text-blue-300">
                  Please wait while files are being copied, verified, and moved
                </p>
              </div>
            )}
          </div>
        )}

        {feedback && !processing && (
          <div className="fixed bottom-6 right-6 bg-slate-700 border border-slate-600 rounded-lg px-6 py-3 shadow-lg">
            {feedback}
          </div>
        )}

        {isStarted && !processing && (
          <div className="mt-6 space-y-4">
            <button
              onClick={resetToStart}
              className="w-full p-3 bg-slate-700 hover:bg-slate-600 rounded-lg font-semibold transition flex items-center justify-center gap-2"
            >
              <ChevronLeft size={20} />
              Start New Session / Sort Another Folder
            </button>

            <div className="bg-slate-800 border border-slate-700 rounded-lg p-4">
              <h3 className="font-semibold mb-2">Keyboard Shortcuts:</h3>
              <ul className="text-sm text-slate-400 space-y-1">
                <li>• Press letter key to classify to that folder</li>
                <li>• Arrow Right or Space: Skip image</li>
                <li>• Arrow Left: Go back</li>
                <li>• Ctrl+S: Save progress manually</li>
                <li>• Mouse wheel on image: Zoom in/out</li>
                <li>• Drag image when zoomed: Pan around</li>
              </ul>
              <p className="text-xs text-slate-500 mt-2">
                Auto-saves every 30 seconds (when changes are made)
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
