import pandas as pd
from sqlalchemy import create_engine

engine = create_engine('postgresql+psycopg2://postgres:neued4%21@localhost:15432/leap_projectdb')
total_money = pd.read_sql("SELECT SUM(total_price) AS money_moved FROM ORDERS WHERE status = 'FILLED'", engine)
print(f"${total_money['money_moved'].values[0]:,.2f}")