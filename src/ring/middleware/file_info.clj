(ns ring.middleware.file-info
  (:use clojure.contrib.def)
  (:import java.io.File))

(defvar- base-mime-types
  {"ai"    "application/postscript"
   "asc"   "text/plain"
   "avi"   "video/x-msvideo"
   "bin"   "application/octet-stream"
   "bmp"   "image/bmp"
   "class" "application/octet-stream"
   "cer"   "application/pkix-cert"
   "crl"   "application/pkix-crl"
   "crt"   "application/x-x509-ca-cert"
   "css"   "text/css"
   "dms"   "application/octet-stream"
   "doc"   "application/msword"
   "dvi"   "application/x-dvi"
   "eps"   "application/postscript"
   "etx"   "text/x-setext"
   "exe"   "application/octet-stream"
   "gif"   "image/gif"
   "htm"   "text/html"
   "html"  "text/html"
   "jpe"   "image/jpeg"
   "jpeg"  "image/jpeg"
   "jpg"   "image/jpeg"
   "js"    "text/javascript"
   "lha"   "application/octet-stream"
   "lzh"   "application/octet-stream"
   "mov"   "video/quicktime"
   "mpe"   "video/mpeg"
   "mpeg"  "video/mpeg"
   "mpg"   "video/mpeg"
   "pbm"   "image/x-portable-bitmap"
   "pdf"   "application/pdf"
   "pgm"   "image/x-portable-graymap"
   "png"   "image/png"
   "pnm"   "image/x-portable-anymap"
   "ppm"   "image/x-portable-pixmap"
   "ppt"   "application/vnd.ms-powerpoint"
   "ps"    "application/postscript"
   "qt"    "video/quicktime"
   "ras"   "image/x-cmu-raster"
   "rb"    "text/plain"
   "rd"    "text/plain"
   "rtf"   "application/rtf"
   "sgm"   "text/sgml"
   "sgml"  "text/sgml"
   "swf"   "application/x-shockwave-flash"
   "tif"   "image/tiff"
   "tiff"  "image/tiff"
   "txt"   "text/plain"
   "xbm"   "image/x-xbitmap"
   "xls"   "application/vnd.ms-excel"
   "xml"   "text/xml"
   "xpm"   "image/x-xpixmap"
   "xwd"   "image/x-xwindowdump"
   "zip"   "application/zip"})

(defn- get-extension
  "Returns the file extension of a file."
  [file]
  (second (re-find #"\.([^./\\]+)$" (.getPath file))))

(defn- guess-mime-type
  "Returns a String corresponding to the guessed mime type for the given file,
  or application/octet-stream if a type cannot be guessed."
  [#^File file mime-types]
  (get mime-types (get-extension file) "application/octet-stream"))

(defn wrap-file-info
  "Wrap an app such that responses with a file a body will have 
  corresponding Content-Type and Content-Length headers added if they are not
  allready present and can be determined from the file. If two arguments are
  given, the second is taken to be a map of file extensions to content types
  that will supplement the default, built-in map."
  [app & [custom-mime-types]]
  (let [mime-types (merge base-mime-types custom-mime-types)]
    (fn [req]
      (let [{:keys [headers body] :as response} (app req)]
        (if (instance? File body)
          (assoc response :headers
            (assoc headers "Content-Length" (str (.length #^File body))
                           "Content-Type"   (guess-mime-type body mime-types)))
          response)))))
