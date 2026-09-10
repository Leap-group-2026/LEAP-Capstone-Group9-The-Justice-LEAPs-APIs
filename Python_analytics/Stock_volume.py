from urllib.parse import quote_plus
import os
import pandas as pd
import matplotlib.pyplot as plt
from sqlalchemy import create_engine

# Database connection setup
password = quote_plus('neued4!')
engine = create_engine(f'postgresql+psycopg2://postgres:{password}@localhost:15432/leap_projectdb')

try:
    # Query the database to get top 5 most traded stocks
    sql_query = """
    SELECT 
        i.ticker,                         
        SUM(o.quantity) as total_volume    -- Sum all quantities traded for each stock
    FROM orders o                          -- Get data from orders table
    JOIN instruments i ON o.instrument_id = i.instrument_id  -- Join with instruments to get ticker
    GROUP BY i.ticker                      -- Group results by stock ticker
    ORDER BY total_volume DESC             -- Sort by volume, highest first
    LIMIT 5;                               -- Get only top 5
    """

    # Fetch the data from database
    df = pd.read_sql(sql_query, engine)

    # Print the data to console
    print("Top 5 Most Traded Stocks:")
    print("-" * 50)
    print(df.to_string(index=False))
    print("-" * 50)

    # Create a bar chart
    plt.figure(figsize=(10, 6))
    bars = plt.bar(df['ticker'], df['total_volume'], color='steelblue', edgecolor='navy', alpha=0.7)

    # Add volume numbers on top of each bar
    for bar in bars:
        height = bar.get_height()
        plt.text(bar.get_x() + bar.get_width()/2., height,
                f'{int(height):,}',
                ha='center', va='bottom', fontsize=10, fontweight='bold')

    # Label the chart
    plt.title('Top 5 Most Traded Stocks by Volume', fontsize=14, fontweight='bold', pad=20)
    plt.xlabel('Stock Ticker', fontsize=12, fontweight='bold')
    plt.ylabel('Trading Volume', fontsize=12, fontweight='bold')
    plt.grid(axis='y', alpha=0.3, linestyle='--')
    plt.tight_layout()

    # Save and display the chart
    script_dir = os.path.dirname(os.path.abspath(__file__))
    output_path = os.path.join(script_dir, 'top_5_traded_stocks.png')
    plt.savefig(output_path, dpi=300, bbox_inches='tight')
    print(f"\nChart saved to {output_path}")
    plt.show()

except Exception as e:
    print(f"Error: {e}")

print("\nDone!")