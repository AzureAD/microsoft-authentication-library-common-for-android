#!/usr/bin/env python3
"""
Script to calculate the average time to merge for pull requests in a GitHub repository.
This version is designed to work with sample data from GitHub MCP server tools.

Usage:
    python3 calculate_pr_merge_time_mcp.py <pr_data.json>
    
    Where pr_data.json contains pull request data from GitHub API
"""

import sys
import json
from datetime import datetime
from typing import List, Dict, Optional
import statistics


class PRMergeTimeCalculator:
    """Calculate average merge time for GitHub pull requests."""
    
    def __init__(self, owner: str, repo: str):
        """
        Initialize the calculator.
        
        Args:
            owner: Repository owner (username or organization)
            repo: Repository name
        """
        self.owner = owner
        self.repo = repo
    
    def calculate_merge_time(self, pr: Dict) -> Optional[float]:
        """
        Calculate the time to merge for a single pull request.
        
        Args:
            pr: Pull request data dictionary
            
        Returns:
            Merge time in hours, or None if calculation fails
        """
        try:
            created_at_str = pr.get("created_at", "")
            merged_at_str = pr.get("merged_at", "")
            
            if not merged_at_str:
                return None
            
            # Parse datetime strings (handle both with and without Z)
            created_at = datetime.fromisoformat(created_at_str.replace("Z", "+00:00"))
            merged_at = datetime.fromisoformat(merged_at_str.replace("Z", "+00:00"))
            
            time_diff = merged_at - created_at
            hours = time_diff.total_seconds() / 3600
            
            return hours
        except (KeyError, ValueError, TypeError) as e:
            print(f"Error calculating merge time for PR #{pr.get('number', 'unknown')}: {e}")
            return None
    
    def format_duration(self, hours: float) -> str:
        """
        Format duration in hours to a human-readable string.
        
        Args:
            hours: Duration in hours
            
        Returns:
            Formatted string (e.g., "2 days, 5 hours")
        """
        days = int(hours // 24)
        remaining_hours = int(hours % 24)
        minutes = int((hours * 60) % 60)
        
        parts = []
        if days > 0:
            parts.append(f"{days} day{'s' if days != 1 else ''}")
        if remaining_hours > 0 or not parts:
            parts.append(f"{remaining_hours} hour{'s' if remaining_hours != 1 else ''}")
        if minutes > 0 and days == 0:
            parts.append(f"{minutes} minute{'s' if minutes != 1 else ''}")
        
        return ", ".join(parts)
    
    def calculate_statistics(self, prs: List[Dict], verbose: bool = True) -> Dict:
        """
        Calculate merge time statistics for pull requests.
        
        Args:
            prs: List of pull request data dictionaries
            verbose: Whether to print detailed information
            
        Returns:
            Dictionary containing statistics
        """
        if verbose:
            print(f"Analyzing pull requests from {self.owner}/{self.repo}...")
        
        # Filter only merged PRs
        merged_prs = [pr for pr in prs if pr.get("merged_at")]
        
        if not merged_prs:
            print("No merged pull requests found.")
            return {}
        
        if verbose:
            print(f"Found {len(merged_prs)} merged pull requests.")
            print("Calculating merge times...\n")
        
        merge_times = []
        pr_details = []
        
        for pr in merged_prs:
            merge_time = self.calculate_merge_time(pr)
            if merge_time is not None and merge_time >= 0:
                merge_times.append(merge_time)
                pr_details.append({
                    "number": pr["number"],
                    "title": pr["title"],
                    "merge_time_hours": merge_time,
                    "author": pr.get("user", {}).get("login", "unknown")
                })
        
        if not merge_times:
            print("Could not calculate merge times for any pull requests.")
            return {}
        
        # Calculate statistics
        avg_time = statistics.mean(merge_times)
        median_time = statistics.median(merge_times)
        min_time = min(merge_times)
        max_time = max(merge_times)
        
        if len(merge_times) > 1:
            stdev_time = statistics.stdev(merge_times)
        else:
            stdev_time = 0
        
        stats = {
            "total_prs_analyzed": len(merge_times),
            "average_hours": avg_time,
            "median_hours": median_time,
            "min_hours": min_time,
            "max_hours": max_time,
            "stdev_hours": stdev_time,
            "pr_details": pr_details
        }
        
        if verbose:
            self.print_statistics(stats)
        
        return stats
    
    def print_statistics(self, stats: Dict):
        """Print statistics in a formatted manner."""
        print("=" * 80)
        print(f"Pull Request Merge Time Statistics for {self.owner}/{self.repo}")
        print("=" * 80)
        print(f"\nTotal PRs Analyzed: {stats['total_prs_analyzed']}")
        print(f"\nAverage Time to Merge: {self.format_duration(stats['average_hours'])}")
        print(f"                       ({stats['average_hours']:.2f} hours)")
        print(f"\nMedian Time to Merge:  {self.format_duration(stats['median_hours'])}")
        print(f"                       ({stats['median_hours']:.2f} hours)")
        print(f"\nFastest Merge:         {self.format_duration(stats['min_hours'])}")
        print(f"                       ({stats['min_hours']:.2f} hours)")
        print(f"\nSlowest Merge:         {self.format_duration(stats['max_hours'])}")
        print(f"                       ({stats['max_hours']:.2f} hours)")
        print(f"\nStandard Deviation:    {stats['stdev_hours']:.2f} hours")
        print("\n" + "=" * 80)
        
        # Show sample PRs
        print("\nFastest Merged Pull Requests:")
        print("-" * 80)
        for pr in sorted(stats['pr_details'], key=lambda x: x['merge_time_hours'])[:10]:
            print(f"  PR #{pr['number']}: {self.format_duration(pr['merge_time_hours'])}")
            print(f"    By: {pr['author']}")
            print(f"    Title: {pr['title'][:65]}{'...' if len(pr['title']) > 65 else ''}")
            print()
        
        if len(stats['pr_details']) > 10:
            print(f"  ... and {len(stats['pr_details']) - 10} more PRs")
        print("=" * 80)


def main():
    """Main function to run the script."""
    if len(sys.argv) < 2:
        print("Usage: python3 calculate_pr_merge_time_mcp.py <pr_data.json>")
        print("\nThis script analyzes PR merge times from GitHub API data.")
        print("Provide a JSON file containing pull request data.")
        sys.exit(1)
    
    # Configuration
    owner = "AzureAD"
    repo = "microsoft-authentication-library-common-for-android"
    
    # Load PR data from file
    json_file = sys.argv[1]
    try:
        with open(json_file, 'r') as f:
            pr_data = json.load(f)
        
        # Handle both list and dict formats
        if isinstance(pr_data, dict):
            # Check if it's a search result or list_pull_requests result
            if "items" in pr_data:
                prs = pr_data["items"]
            elif "data" in pr_data:
                prs = pr_data["data"]
            else:
                prs = [pr_data]
        else:
            prs = pr_data
        
        if not prs:
            print("No pull request data found in the file.")
            sys.exit(1)
        
        # Create calculator instance
        calculator = PRMergeTimeCalculator(owner, repo)
        
        # Calculate and display statistics
        calculator.calculate_statistics(prs, verbose=True)
        
    except FileNotFoundError:
        print(f"Error: File '{json_file}' not found.")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"Error: Invalid JSON in file '{json_file}': {e}")
        sys.exit(1)
    except KeyboardInterrupt:
        print("\n\nOperation cancelled by user.")
        sys.exit(0)
    except Exception as e:
        print(f"\nError: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
