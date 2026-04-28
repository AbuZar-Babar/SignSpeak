#!/usr/bin/env python

import sys
from src.analysis.run import run_full_analysis

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Run comprehensive EDA analysis")
    parser.add_argument("--output", type=str, default="analysis_report.json")
    args = parser.parse_args()
    report = run_full_analysis(args.output)
    sys.exit(0 if report else 1)
