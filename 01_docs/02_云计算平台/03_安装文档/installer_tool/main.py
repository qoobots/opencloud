# -*- coding: utf-8 -*-
"""
OpenCloud 安装工具 - 主入口
"""

import sys
import os

# 将 src 目录加入 Python 路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "src"))

from gui import main

if __name__ == "__main__":
    main()
