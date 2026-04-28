#!/usr/bin/env python

import sys
import json
import argparse
from pathlib import Path
from datetime import datetime

project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from src.config.config import DATA_SOURCES, ACTIONS_FILE
from src.data.ingestion import load_data
from src.analysis import (
    DataQualityAnalyzer,
    FeatureAnalyzer,
    compare_sources,
    validate_augmented_samples,
)
from src.features.feature_engineering import SignLanguageAugmenter


def load_actions() -> list:
    with open(ACTIONS_FILE, 'r') as f:
        return [line.strip() for line in f.readlines() if line.strip()]


def run_full_analysis(output_file: str = "analysis_report.json"):
    print("\n" + "="*70)
    print("SignSpeak ML Pipeline - Comprehensive EDA Analysis")
    print("="*70 + "\n")
    
    try:
        print("[1/5] Loading data...")
        actions = load_actions()
        X, y = load_data(actions, [DATA_SOURCES["laptop"], DATA_SOURCES["mobile"]])
        print(f"      Loaded {X.shape[0]} sequences, {len(set(y))} classes")
        
        print("\n[2/5] Running data quality checks...")
        dq_analyzer = DataQualityAnalyzer()
        dq_report = dq_analyzer.generate_full_report(X, y, actions)
        
        print("[3/5] Computing feature statistics...")
        feat_analyzer = FeatureAnalyzer()
        feat_stats = feat_analyzer.per_landmark_statistics(X, y, actions)
        print(f"      Analyzed {feat_stats['num_landmarks']} landmarks")
        print(f"      Across {feat_stats['num_frames']} total frames")
        
        print("\n[4/5] Comparing laptop vs mobile sources...")
        try:
            X_lap, y_lap = load_data(actions, [DATA_SOURCES["laptop"]])
            X_mob, y_mob = load_data(actions, [DATA_SOURCES["mobile"]])
            cross_source_report = compare_sources(X_lap, y_lap, X_mob, y_mob, actions)
            print(f"      Domain gap score: {cross_source_report['domain_gap_score']:.4f}")
            print(f"      Significant differences: {cross_source_report['significant_differences']}")
        except Exception as e:
            print(f"      Warning: Could not compare sources - {e}")
            cross_source_report = None
        
        print("\n[5/5] Validating augmentation quality...")
        augmenter = SignLanguageAugmenter()
        X_aug, y_aug = augmenter.create_augmented_dataset(X, y, multiplier=2)
        aug_validation = validate_augmented_samples(X_aug, y_aug)
        print(f"      Generated {X_aug.shape[0]} augmented samples")
        print(f"      Valid samples: {aug_validation['valid_samples']}/{aug_validation['total_samples']}")
        
        report = {
            "timestamp": datetime.now().isoformat(),
            "dataset": {
                "total_sequences": X.shape[0],
                "frames_per_sequence": X.shape[1],
                "features_per_frame": X.shape[2],
                "num_classes": len(set(y)),
                "num_landmarks": 42
            },
            "data_quality": dq_report,
            "feature_statistics": feat_stats,
            "augmentation_validation": aug_validation,
        }
        
        if cross_source_report:
            report["cross_source_analysis"] = cross_source_report
        
        with open(output_file, 'w') as f:
            json.dump(report, f, indent=2)
        
        print("\n" + "="*70)
        print(f"Analysis complete! Report saved to: {output_file}")
        print("="*70 + "\n")
        
        return report
        
    except Exception as e:
        print(f"\nERROR: Analysis failed - {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        return None


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Run comprehensive EDA and data quality analysis"
    )
    parser.add_argument(
        "--output",
        type=str,
        default="analysis_report.json",
        help="Path to save analysis report (default: analysis_report.json)"
    )
    
    args = parser.parse_args()
    report = run_full_analysis(args.output)
    sys.exit(0 if report else 1)
