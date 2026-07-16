"""
Helper functions to convert base64 strings (sent from React via Spring Boot)
into OpenCV-readable images, and back if ever needed.
"""

import base64
import numpy as np
import cv2


def decode_base64_image(base64_str: str):
    """
    Converts a base64 string (optionally with a 'data:image/jpeg;base64,' prefix,
    as browsers usually send from <canvas>.toDataURL()) into an OpenCV BGR image.

    Returns None if decoding fails.
    """
    try:
        if "," in base64_str:
            base64_str = base64_str.split(",")[1]

        img_bytes = base64.b64decode(base64_str)
        np_arr = np.frombuffer(img_bytes, np.uint8)
        img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
        return img
    except Exception:
        return None
