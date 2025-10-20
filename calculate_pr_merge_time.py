#!/usr/bin/env python3
"""
Script to calculate the average time to merge for pull requests in a GitHub repository.

This script queries the GitHub API to fetch closed pull requests that were merged,
calculates the time difference between when each PR was created and when it was merged,
and computes the average merge time.

Usage:
    python3 calculate_pr_merge_time.py

Requirements:
    - Python 3.6+
    - requests library (pip install requests)
    - GitHub personal access token (optional, for higher API rate limits)
      Set as GITHUB_TOKEN environment variable
"""

import os
import sys
import json
from datetime import datetime
from typing import List, Dict, Optional
import statistics

try:
    import requests
except ImportError:
    print("Error: 'requests' library not found.")
    print("Please install it using: pip install requests")
    sys.exit(1)


class PRMergeTimeCalculator:
    """Calculate average merge time for GitHub pull requests."""
    
    def __init__(self, owner: str, repo: str, token: Optional[str] = None):
        """
        Initialize the calculator.
        
        Args:
            owner: Repository owner (username or organization)
            repo: Repository name
            token: GitHub personal access token (optional)
        """
        self.owner = owner
        self.repo = repo
        self.token = token
        self.base_url = "https://api.github.com"
        self.headers = {
            "Accept": "application/vnd.github.v3+json"
        }
        if self.token:
            self.headers["Authorization"] = f"token {self.token}"
    
    def fetch_merged_pull_requests(self, max_prs: int = 100) -> List[Dict]:
        """
        Fetch merged pull requests from the repository.
        
        Args:
            max_prs: Maximum number of PRs to fetch (default: 100)
            
        Returns:
            List of pull request data dictionaries
        """
        url = f"{self.base_url}/repos/{self.owner}/{self.repo}/pulls"
        params = {
            "state": "closed",
            "per_page": min(max_prs, 100),
            "sort": "updated",
            "direction": "desc"
        }
        
        merged_prs = []
        page = 1
        
        while len(merged_prs) < max_prs:
            params["page"] = page
            try:
                response = requests.get(url, headers=self.headers, params=params, timeout=30)
                response.raise_for_status()
                prs = response.json()
                
                if not prs:
                    break
                
                # Filter only merged PRs
                for pr in prs:
                    if pr.get("merged_at"):
                        merged_prs.append(pr)
                        if len(merged_prs) >= max_prs:
                            break
                
                page += 1
                
                # Check rate limit
                if "X-RateLimit-Remaining" in response.headers:
                    remaining = int(response.headers["X-RateLimit-Remaining"])
                    if remaining < 10:
                        print(f"Warning: Only {remaining} API requests remaining")
                        
            except requests.exceptions.RequestException as e:
                print(f"Error fetching pull requests: {e}")
                break
        
        return merged_prs
    
    def calculate_merge_time(self, pr: Dict) -> Optional[float]:
        """
        Calculate the time to merge for a single pull request.
        
        Args:
            pr: Pull request data dictionary
            
        Returns:
            Merge time in hours, or None if calculation fails
        """
        try:
            created_at = datetime.strptime(pr["created_at"], "%Y-%m-%dT%H:%M:%SZ")
            merged_at = datetime.strptime(pr["merged_at"], "%Y-%m-%dT%H:%M:%SZ")
            
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
    
    def calculate_statistics(self, max_prs: int = 100, verbose: bool = True) -> Dict:
        """
        Calculate merge time statistics for pull requests.
        
        Args:
            max_prs: Maximum number of PRs to analyze
            verbose: Whether to print detailed information
            
        Returns:
            Dictionary containing statistics
        """
        if verbose:
            print(f"Fetching merged pull requests from {self.owner}/{self.repo}...")
        
        merged_prs = self.fetch_merged_pull_requests(max_prs)
        
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
            if merge_time is not None:
                merge_times.append(merge_time)
                pr_details.append({
                    "number": pr["number"],
                    "title": pr["title"],
                    "merge_time_hours": merge_time
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
        print("\nSample of Recent Merged Pull Requests:")
        print("-" * 80)
        for pr in stats['pr_details'][:10]:
            print(f"  PR #{pr['number']}: {self.format_duration(pr['merge_time_hours'])}")
            print(f"    Title: {pr['title'][:70]}{'...' if len(pr['title']) > 70 else ''}")
        
        if len(stats['pr_details']) > 10:
            print(f"\n  ... and {len(stats['pr_details']) - 10} more PRs")
        print("=" * 80)


def main():
    """Main function to run the script."""
    # Configuration
    owner = "AzureAD"
    repo = "microsoft-authentication-library-common-for-android"
    
    # Get GitHub token from environment variable (optional)
    token = os.environ.get("GITHUB_TOKEN")
    
    if not token:
        print("Note: No GITHUB_TOKEN environment variable found.")
        print("Running without authentication (lower API rate limits).")
        print("To increase rate limits, set GITHUB_TOKEN environment variable.\n")
    
    # Create calculator instance
    calculator = PRMergeTimeCalculator(owner, repo, token)
    
    # Calculate and display statistics
    try:
        calculator.calculate_statistics(max_prs=100, verbose=True)
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
